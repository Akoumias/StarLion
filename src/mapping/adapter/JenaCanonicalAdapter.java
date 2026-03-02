package mapping.adapter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.forth.ics.rdfsuite.services.RdfDocument;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import mapping.canonical.CanonicalEdge;
import mapping.canonical.CanonicalElementType;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalNode;
import mapping.canonical.CanonicalRdfSnapshot;

/**
 * Maps Jena model output to the same canonical DTOs used for SWKM output.
 *
 * @author jakoum
 */
public class JenaCanonicalAdapter {

    /**
     * Loads a file and converts it to canonical RDF snapshot form.
     */
    public CanonicalRdfSnapshot fromFile(String modelFile, String baseUri, String format) {
        try (InputStream fileStream = new FileInputStream(modelFile)) {
            return fromInputStream(fileStream, baseUri, format);
        } catch (java.io.IOException ioException) {
            throw new IllegalStateException("Unable to read RDF file for Jena mapping: " + modelFile, ioException);
        }
    }

    /**
     * Parses one RDF payload and maps it to canonical RDF collections.
     */
    public CanonicalRdfSnapshot fromInputStream(InputStream input, String baseUri, String format) {
        byte[] payload = readBytes(input);
        Model model = parseModel(payload, baseUri, format);
        return fromJenaModel(model);
    }

    /**
     * Merges multiple project documents into a single Jena model before mapping.
     */
    public CanonicalRdfSnapshot fromRdfDocuments(Iterable<RdfDocument> documents) {
        Model model = ModelFactory.createDefaultModel();
        for (RdfDocument document : documents) {
            if (document == null || document.getContent() == null) {
                continue;
            }
            byte[] payload = document.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Model parsed = parseModel(payload, document.getURI(), String.valueOf(document.getFormat()));
            model.add(parsed);
        }
        return fromJenaModel(model);
    }

    /**
     * Extracts normalized namespaces/classes/properties/instances from Jena model.
     */
    public CanonicalRdfSnapshot fromJenaModel(Model model) {
        Set<String> namespaces = new HashSet<>();
        Set<String> classes = new HashSet<>();
        Set<String> properties = new HashSet<>();
        Set<String> instances = new HashSet<>();
        Set<String> propertyInstances = new HashSet<>();

        for (Map.Entry<String, String> ns : model.getNsPrefixMap().entrySet()) {
            if (ns.getValue() != null && !ns.getValue().isEmpty()) {
                namespaces.add(CanonicalNameUtils.namespace(ns.getValue()));
            }
        }

        bootstrapVocabularySafely(classes, properties, namespaces);

        ResIterator classIterator = model.listResourcesWithProperty(RDF.type, RDFS.Class);
        while (classIterator.hasNext()) {
            Resource resource = classIterator.next();
            if (resource.isURIResource()) {
                String uri = CanonicalNameUtils.normalizeUri(resource.getURI());
                classes.add(uri);
                namespaces.add(CanonicalNameUtils.namespace(uri));
            }
        }

        ResIterator owlClassIterator = model.listResourcesWithProperty(RDF.type, OWL.Class);
        while (owlClassIterator.hasNext()) {
            Resource resource = owlClassIterator.next();
            if (resource.isURIResource()) {
                String uri = CanonicalNameUtils.normalizeUri(resource.getURI());
                classes.add(uri);
                namespaces.add(CanonicalNameUtils.namespace(uri));
            }
        }

        ResIterator propertyIterator = model.listResourcesWithProperty(RDF.type, RDF.Property);
        while (propertyIterator.hasNext()) {
            Resource resource = propertyIterator.next();
            if (resource.isURIResource()) {
                String uri = CanonicalNameUtils.normalizeUri(resource.getURI());
                properties.add(uri);
                namespaces.add(CanonicalNameUtils.namespace(uri));
            }
        }
        ResIterator owlObjectPropertyIterator = model.listResourcesWithProperty(RDF.type, OWL.ObjectProperty);
        while (owlObjectPropertyIterator.hasNext()) {
            Resource resource = owlObjectPropertyIterator.next();
            if (resource.isURIResource()) {
                String uri = CanonicalNameUtils.normalizeUri(resource.getURI());
                properties.add(uri);
                namespaces.add(CanonicalNameUtils.namespace(uri));
            }
        }
        ResIterator owlDatatypePropertyIterator = model.listResourcesWithProperty(RDF.type, OWL.DatatypeProperty);
        while (owlDatatypePropertyIterator.hasNext()) {
            Resource resource = owlDatatypePropertyIterator.next();
            if (resource.isURIResource()) {
                String uri = CanonicalNameUtils.normalizeUri(resource.getURI());
                properties.add(uri);
                namespaces.add(CanonicalNameUtils.namespace(uri));
            }
        }

        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.nextStatement();
            Resource subject = statement.getSubject();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();

            if (subject.isURIResource()) {
                String subjectUri = CanonicalNameUtils.normalizeUri(subject.getURI());
                namespaces.add(CanonicalNameUtils.namespace(subjectUri));
            }
            if (predicate != null && predicate.getURI() != null) {
                String predicateUri = CanonicalNameUtils.normalizeUri(predicate.getURI());
                properties.add(predicateUri);
                namespaces.add(CanonicalNameUtils.namespace(predicateUri));
            }

            if (RDF.type.equals(predicate) && object.isURIResource()) {
                Resource objectResource = object.asResource();
                String objectUri = CanonicalNameUtils.normalizeUri(objectResource.getURI());
                if (objectUri != null
                        && !RDFS.Class.getURI().equals(objectUri)
                        && !OWL.Class.getURI().equals(objectUri)
                        && !RDF.Property.getURI().equals(objectUri)
                        && !OWL.ObjectProperty.getURI().equals(objectUri)
                        && !OWL.DatatypeProperty.getURI().equals(objectUri)) {
                    if (subject.isURIResource()) {
                        instances.add(CanonicalNameUtils.normalizeUri(subject.getURI()));
                    }
                }
                classes.add(objectUri);
                namespaces.add(CanonicalNameUtils.namespace(objectUri));
            } else if (RDFS.subClassOf.equals(predicate)) {
                if (subject.isURIResource()) {
                    classes.add(CanonicalNameUtils.normalizeUri(subject.getURI()));
                }
                if (object.isURIResource()) {
                    classes.add(CanonicalNameUtils.normalizeUri(object.asResource().getURI()));
                }
                String subjectUri = subject.isURIResource()
                        ? CanonicalNameUtils.normalizeUri(subject.getURI())
                        : CanonicalNameUtils.normalizeUri(subject.toString());
                String objectUri = object.isURIResource()
                        ? CanonicalNameUtils.normalizeUri(object.asResource().getURI())
                        : CanonicalNameUtils.normalizeUri(object.toString());
                propertyInstances.add(subjectUri + "|" + RDFS.subClassOf.getURI() + "|" + objectUri);
            } else if (RDFS.domain.equals(predicate) || RDFS.range.equals(predicate)) {
                if (subject.isURIResource()) {
                    properties.add(CanonicalNameUtils.normalizeUri(subject.getURI()));
                }
                if (object.isURIResource()) {
                    String objectUri = CanonicalNameUtils.normalizeUri(object.asResource().getURI());
                    classes.add(objectUri);
                    namespaces.add(CanonicalNameUtils.namespace(objectUri));
                }
                String subjectUri = subject.isURIResource()
                        ? CanonicalNameUtils.normalizeUri(subject.getURI())
                        : CanonicalNameUtils.normalizeUri(subject.toString());
                String objectUri = object.isURIResource()
                        ? CanonicalNameUtils.normalizeUri(object.asResource().getURI())
                        : CanonicalNameUtils.normalizeUri(object.toString());
                propertyInstances.add(subjectUri + "|" + CanonicalNameUtils.normalizeUri(predicate.getURI()) + "|" + objectUri);
            } else {
                String subjectUri = subject.isURIResource()
                        ? CanonicalNameUtils.normalizeUri(subject.getURI())
                        : CanonicalNameUtils.normalizeUri(subject.toString());
                String objectValue = object.isURIResource()
                        ? CanonicalNameUtils.normalizeUri(object.asResource().getURI())
                        : CanonicalNameUtils.normalizeUri(object.toString());
                String predicateUri = CanonicalNameUtils.normalizeUri(predicate.getURI());
                if (!isAnnotationPredicate(predicateUri)) {
                    propertyInstances.add(subjectUri + "|" + predicateUri + "|" + objectValue);
                }
            }

            if (object.isURIResource()) {
                String objectUri = CanonicalNameUtils.normalizeUri(object.asResource().getURI());
                namespaces.add(CanonicalNameUtils.namespace(objectUri));
            }
        }

        return new CanonicalRdfSnapshot(
                new ArrayList<>(namespaces),
                new ArrayList<>(classes),
                new ArrayList<>(properties),
                new ArrayList<>(instances),
                new ArrayList<>(propertyInstances)
        );
    }

    /**
     * Builds graph DTO nodes/edges from canonical RDF triples and schema hints.
     */
    public CanonicalGraphSnapshot toGraphSnapshot(CanonicalRdfSnapshot rdfSnapshot) {
        Set<String> nodeIds = new HashSet<>();
        List<CanonicalNode> nodes = new ArrayList<>();
        List<CanonicalEdge> edges = new ArrayList<>();
        java.util.Map<String, Set<String>> propertyDomains = new java.util.HashMap<>();
        java.util.Map<String, Set<String>> propertyRanges = new java.util.HashMap<>();
        java.util.Map<String, String> subPropertyParent = new java.util.HashMap<>();
        java.util.Map<String, Set<String>> subPropertiesByParent = new java.util.HashMap<>();
        List<String[]> subclassOfRelations = new ArrayList<>();

        List<String> sortedClasses = new ArrayList<>(rdfSnapshot.getClasses());
        sortedClasses.sort((left, right) -> Integer.compare(classPriority(left), classPriority(right)));
        for (String classUri : sortedClasses) {
            String className = CanonicalNameUtils.localName(classUri);
            if (nodeIds.add(className)) {
                nodes.add(new CanonicalNode(
                        className,
                        className,
                        classUri,
                        CanonicalNameUtils.namespace(classUri),
                        CanonicalElementType.CLASS,
                        true,
                        false
                ));
            }
        }

        for (String instanceUri : rdfSnapshot.getInstances()) {
            String instanceName = CanonicalNameUtils.localName(instanceUri);
            if (nodeIds.add(instanceName)) {
                nodes.add(new CanonicalNode(
                        instanceName,
                        instanceName,
                        instanceUri,
                        CanonicalNameUtils.namespace(instanceUri),
                        CanonicalElementType.CLASS_INSTANCE,
                        true,
                        false
                ));
            }
        }

        for (String stmt : rdfSnapshot.getPropertyInstances()) {
            String[] parts = stmt.split("\\|", 3);
            if (parts.length < 3) {
                continue;
            }
            String sourceUri = CanonicalNameUtils.normalizeUri(parts[0]);
            String predicateUri = CanonicalNameUtils.normalizeUri(parts[1]);
            String targetUri = CanonicalNameUtils.normalizeUri(parts[2]);
            String sourceId = CanonicalNameUtils.localName(sourceUri);
            String targetId = CanonicalNameUtils.localName(targetUri);

            if (RDFS.subClassOf.getURI().equals(predicateUri)) {
                if (!sourceId.isEmpty() && !targetId.isEmpty()) {
                    subclassOfRelations.add(new String[]{sourceUri, targetUri});
                }
                continue;
            }
            if (RDFS.subPropertyOf.getURI().equals(predicateUri)) {
                subPropertyParent.put(sourceUri, targetUri);
                subPropertiesByParent
                        .computeIfAbsent(targetUri, ignored -> new HashSet<>())
                        .add(CanonicalNameUtils.localName(sourceUri));
                continue;
            }
            if (RDFS.domain.getURI().equals(predicateUri)) {
                propertyDomains.computeIfAbsent(sourceUri, ignored -> new HashSet<>()).add(targetUri);
                continue;
            }
            if (RDFS.range.getURI().equals(predicateUri)) {
                propertyRanges.computeIfAbsent(sourceUri, ignored -> new HashSet<>()).add(targetUri);
                continue;
            }

            if (sourceId.isEmpty() || targetId.isEmpty()) {
                continue;
            }

            if (nodeIds.add(sourceId)) {
                nodes.add(new CanonicalNode(
                        sourceId,
                        sourceId,
                    sourceUri,
                    CanonicalNameUtils.namespace(sourceUri),
                        CanonicalElementType.CLASS_INSTANCE,
                        true,
                        false
                ));
            }
            if (nodeIds.add(targetId)) {
                nodes.add(new CanonicalNode(
                        targetId,
                        targetId,
                        targetUri,
                        CanonicalNameUtils.namespace(targetUri),
                        CanonicalElementType.CLASS_INSTANCE,
                        true,
                        false
                ));
            }

            String edgeId = sourceId + "|" + predicateUri + "|" + targetId;
            edges.add(new CanonicalEdge(
                    edgeId,
                    CanonicalNameUtils.localName(predicateUri),
                    sourceId,
                    targetId,
                    CanonicalElementType.PROPERTY_INSTANCE,
                    true,
                    true,
                    java.util.Collections.<String>emptyList()
            ));
        }

        addCoreSchemaMappings(propertyDomains, propertyRanges, subclassOfRelations);

        for (String[] rel : subclassOfRelations) {
            String childId = CanonicalNameUtils.localName(rel[0]);
            String parentId = CanonicalNameUtils.localName(rel[1]);
            if (childId.isEmpty() || parentId.isEmpty()) {
                continue;
            }
            if (nodeIds.add(childId)) {
                nodes.add(new CanonicalNode(
                        childId, childId, rel[0], CanonicalNameUtils.namespace(rel[0]),
                        CanonicalElementType.CLASS, true, false
                ));
            }
            if (nodeIds.add(parentId)) {
                nodes.add(new CanonicalNode(
                        parentId, parentId, rel[1], CanonicalNameUtils.namespace(rel[1]),
                        CanonicalElementType.CLASS, true, false
                ));
            }
            edges.add(new CanonicalEdge(
                    "isA" + childId + parentId,
                    "isA",
                    childId,
                    parentId,
                    CanonicalElementType.SUBCLASS_OF,
                    true,
                    true,
                    java.util.Collections.<String>emptyList()
            ));
        }

        for (String propertyUri : propertyDomains.keySet()) {
            Set<String> domains = propertyDomains.get(propertyUri);
            Set<String> ranges = propertyRanges.get(propertyUri);
            if (domains == null || domains.isEmpty() || ranges == null || ranges.isEmpty()) {
                continue;
            }
            String propertyLabel = CanonicalNameUtils.localName(propertyUri);
            for (String domainUri : domains) {
                String domainId = CanonicalNameUtils.localName(domainUri);
                if (domainId.isEmpty()) {
                    continue;
                }
                if (nodeIds.add(domainId)) {
                    nodes.add(new CanonicalNode(
                            domainId, domainId, domainUri, CanonicalNameUtils.namespace(domainUri),
                            CanonicalElementType.CLASS, true, false
                    ));
                }
                for (String rangeUri : ranges) {
                    String rangeId = CanonicalNameUtils.localName(rangeUri);
                    if (rangeId.isEmpty()) {
                        continue;
                    }
                    if (nodeIds.add(rangeId)) {
                        nodes.add(new CanonicalNode(
                                rangeId, rangeId, rangeUri, CanonicalNameUtils.namespace(rangeUri),
                                CanonicalElementType.CLASS, true, false
                        ));
                    }
                    edges.add(new CanonicalEdge(
                            propertyLabel + domainId + rangeId,
                            propertyLabel,
                            domainId,
                            rangeId,
                            CanonicalElementType.PROPERTY,
                            true,
                            true,
                            collectSubPropertiesForProperty(propertyUri, propertyLabel, subPropertiesByParent)
                    ));
                }
            }
        }

        return new CanonicalGraphSnapshot(nodes, edges, rdfSnapshot.getNamespaces());
    }

    private String toJenaLang(String swkmFormat) {
        if (swkmFormat == null) {
            return "RDF/XML";
        }
        String normalized = swkmFormat.trim().toUpperCase();
        if (normalized.contains("TRIG")) {
            return "TRIG";
        }
        if (normalized.contains("TURTLE")) {
            return "TURTLE";
        }
        if (normalized.contains("N3")) {
            return "N3";
        }
        if (normalized.contains("N-TRIPLES") || normalized.contains("NTRIPLES")) {
            return "N-TRIPLE";
        }
        if (normalized.contains("RDF/XML") || normalized.contains("RDF_XML")) {
            return "RDF/XML";
        }
        return swkmFormat;
    }

    /**
     * Attempts parsing with preferred format first, then common RDF syntaxes.
     */
    private Model parseModel(byte[] payload, String baseUri, String preferredFormat) {
        Model model = ModelFactory.createDefaultModel();
        List<Lang> candidates = new ArrayList<Lang>();
        Lang preferredLang = toLang(preferredFormat);
        if (preferredLang != null) {
            candidates.add(preferredLang);
        }
        candidates.add(Lang.RDFXML);
        candidates.add(Lang.TURTLE);
        candidates.add(Lang.TRIG);
        candidates.add(Lang.N3);
        candidates.add(Lang.NTRIPLES);

        RuntimeException last = null;
        Set<Lang> seen = new HashSet<Lang>();
        for (Lang lang : candidates) {
            if (lang == null || !seen.add(lang)) {
                continue;
            }
            try {
                Model attempt = ModelFactory.createDefaultModel();
                RDFParser.create()
                        .source(new ByteArrayInputStream(payload))
                        .lang(lang)
                        .base(baseUri)
                        .parse(attempt.getGraph());
                return attempt;
            } catch (RuntimeException ex) {
                last = ex;
            }
        }

        if (last != null) {
            throw last;
        }
        return model;
    }

    private Lang toLang(String format) {
        if (format == null) {
            return null;
        }
        String normalized = format.trim().toUpperCase();
        if (normalized.contains("RDF/XML") || normalized.contains("RDF_XML")) {
            return Lang.RDFXML;
        }
        if (normalized.contains("TURTLE")) {
            return Lang.TURTLE;
        }
        if (normalized.contains("TRIG")) {
            return Lang.TRIG;
        }
        if (normalized.contains("N3")) {
            return Lang.N3;
        }
        if (normalized.contains("N-TRIPLES") || normalized.contains("NTRIPLES")) {
            return Lang.NTRIPLES;
        }
        return null;
    }

    private byte[] readBytes(InputStream input) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException ioException) {
            throw new IllegalStateException("Unable to read RDF payload", ioException);
        }
    }

    private void bootstrapVocabulary(Set<String> classes, Set<String> properties, Set<String> namespaces) {
        namespaces.add(RDF.getURI());
        namespaces.add(RDFS.getURI());
        namespaces.add(XSD.getURI());

        classes.add(CanonicalNameUtils.normalizeUri(RDF.Alt.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDF.Bag.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDF.Seq.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDF.Statement.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDF.List.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDF.Property.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDFS.Class.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDFS.Resource.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDFS.Literal.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDFS.Datatype.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDFS.Container.getURI()));
        classes.add(CanonicalNameUtils.normalizeUri(RDFS.ContainerMembershipProperty.getURI()));

        properties.add(CanonicalNameUtils.normalizeUri(RDF.type.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDF.subject.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDF.predicate.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDF.object.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDF.first.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDF.getURI() + "li"));
        properties.add(CanonicalNameUtils.normalizeUri(RDF.value.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.subClassOf.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.subPropertyOf.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.domain.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.range.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.label.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.comment.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.member.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.seeAlso.getURI()));
        properties.add(CanonicalNameUtils.normalizeUri(RDFS.isDefinedBy.getURI()));

        addResourceUri(classes, XSD.xfloat);
        addResourceUri(classes, XSD.xdouble);
        addResourceUri(classes, XSD.xint);
        addResourceUri(classes, XSD.xlong);
        addResourceUri(classes, XSD.xshort);
        addResourceUri(classes, XSD.xbyte);
        addResourceUri(classes, XSD.xboolean);
        addResourceUri(classes, XSD.xstring);
        addResourceUri(classes, XSD.unsignedByte);
        addResourceUri(classes, XSD.unsignedShort);
        addResourceUri(classes, XSD.unsignedInt);
        addResourceUri(classes, XSD.unsignedLong);
        addResourceUri(classes, XSD.decimal);
        addResourceUri(classes, XSD.integer);
        addResourceUri(classes, XSD.nonPositiveInteger);
        addResourceUri(classes, XSD.nonNegativeInteger);
        addResourceUri(classes, XSD.positiveInteger);
        addResourceUri(classes, XSD.negativeInteger);
        addResourceUri(classes, XSD.normalizedString);
        addResourceUri(classes, XSD.anyURI);
        addResourceUri(classes, XSD.token);
        addResourceUri(classes, XSD.Name);
        addResourceUri(classes, XSD.QName);
        addResourceUri(classes, XSD.language);
        addResourceUri(classes, XSD.NMTOKEN);
        addResourceUri(classes, XSD.ENTITIES);
        addResourceUri(classes, XSD.NMTOKENS);
        addResourceUri(classes, XSD.ENTITY);
        addResourceUri(classes, XSD.ID);
        addResourceUri(classes, XSD.NCName);
        addResourceUri(classes, XSD.IDREF);
        addResourceUri(classes, XSD.IDREFS);
        addResourceUri(classes, XSD.NOTATION);
        addResourceUri(classes, XSD.hexBinary);
        addResourceUri(classes, XSD.base64Binary);
        addResourceUri(classes, XSD.date);
        addResourceUri(classes, XSD.time);
        addResourceUri(classes, XSD.dateTime);
        addResourceUri(classes, XSD.duration);
        addResourceUri(classes, XSD.gDay);
        addResourceUri(classes, XSD.gMonth);
        addResourceUri(classes, XSD.gYear);
        addResourceUri(classes, XSD.gYearMonth);
        addResourceUri(classes, XSD.gMonthDay);

        namespaces.add("http://139.91.183.30:9090/RDF/rdfsuite.rdfs#");
        classes.add("http://139.91.183.30:9090/RDF/rdfsuite.rdfs#Graph");
        classes.add("http://139.91.183.30:9090/RDF/rdfsuite.rdfs#LiteralType");
        classes.add("http://www.w3.org/2001/XMLSchema#ENTITIES");
        classes.add("http://www.w3.org/2001/XMLSchema#NMTOKENS");
        classes.add("http://www.w3.org/2001/XMLSchema#IDREFS");
        classes.add("http://www.w3.org/2000/01/rdf-schema#ConstraintProperty");
        classes.add("http://www.w3.org/2000/01/rdf-schema#ConstraintResource");
    }

    private void bootstrapVocabularySafely(Set<String> classes, Set<String> properties, Set<String> namespaces) {
        try {
            bootstrapVocabulary(classes, properties, namespaces);
        } catch (RuntimeException ignored) {
            // Keep parity runnable even if a vocabulary constant is unavailable.
        }
    }

    private boolean isAnnotationPredicate(String predicateUri) {
        return RDFS.comment.getURI().equals(predicateUri)
                || RDFS.label.getURI().equals(predicateUri)
                || RDFS.seeAlso.getURI().equals(predicateUri)
                || RDFS.isDefinedBy.getURI().equals(predicateUri);
    }

    private void addResourceUri(Set<String> sink, org.apache.jena.rdf.model.Resource resource) {
        if (resource == null || resource.getURI() == null) {
            return;
        }
        sink.add(CanonicalNameUtils.normalizeUri(resource.getURI()));
    }

    private void addCoreSchemaMappings(
            java.util.Map<String, Set<String>> propertyDomains,
            java.util.Map<String, Set<String>> propertyRanges,
            List<String[]> subclassOfRelations
    ) {
        addDomainRange(propertyDomains, propertyRanges, RDF.subject.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDF.predicate.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDF.object.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDF.first.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDF.getURI() + "li", RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDF.value.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDF.type.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.comment.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.domain.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.range.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.label.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.seeAlso.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.isDefinedBy.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.subClassOf.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.subPropertyOf.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());
        addDomainRange(propertyDomains, propertyRanges, RDFS.member.getURI(), RDFS.Resource.getURI(), RDFS.Resource.getURI());

        subclassOfRelations.add(new String[]{
                "http://139.91.183.30:9090/RDF/rdfsuite.rdfs#Graph",
                RDFS.Resource.getURI()
        });
        subclassOfRelations.add(new String[]{
                "http://139.91.183.30:9090/RDF/rdfsuite.rdfs#LiteralType",
                RDFS.Class.getURI()
        });
    }

    private void addDomainRange(
            java.util.Map<String, Set<String>> propertyDomains,
            java.util.Map<String, Set<String>> propertyRanges,
            String propertyUri,
            String domainUri,
            String rangeUri
    ) {
        String property = CanonicalNameUtils.normalizeUri(propertyUri);
        String domain = CanonicalNameUtils.normalizeUri(domainUri);
        String range = CanonicalNameUtils.normalizeUri(rangeUri);
        propertyDomains.computeIfAbsent(property, ignored -> new HashSet<>()).add(domain);
        propertyRanges.computeIfAbsent(property, ignored -> new HashSet<>()).add(range);
    }

    private List<String> collectSubPropertiesForProperty(
            String propertyUri,
            String propertyLabel,
            java.util.Map<String, Set<String>> subPropertiesByParent
    ) {
        Set<String> result = new HashSet<>(subPropertiesByParent.getOrDefault(propertyUri, java.util.Collections.<String>emptySet()));
        for (String parentUri : subPropertiesByParent.keySet()) {
            if (propertyLabel.equals(CanonicalNameUtils.localName(parentUri))) {
                result.addAll(subPropertiesByParent.get(parentUri));
            }
        }
        return new ArrayList<>(result);
    }

    private int classPriority(String classUri) {
        if (classUri == null) {
            return 100;
        }
        if (classUri.contains("rdfsuite.rdfs#Graph") || classUri.contains("rdfsuite.rdfs#LiteralType")) {
            return 0;
        }
        if (classUri.startsWith("http://www.w3.org/")) {
            return 1;
        }
        return 2;
    }

}
