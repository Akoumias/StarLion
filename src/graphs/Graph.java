package graphs;

import gr.forth.ics.rdfsuite.services.Format;
import gr.forth.ics.rdfsuite.services.util.QueryUtils;
import gr.forth.ics.swkmclient.Client;
import graphs.save.GraphState;
import gui.MainFrame;
import gui.Project;
import gui.Project.DEPENDENCIES;
import gui.ProjectManager;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import model.RDFClass;
import model.RDFModel;
import model.RDFNamespace;
import model.RDFProperty;
import model.RDFResource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JDialog;
import javax.swing.JLabel;
import mapping.canonical.CanonicalEdge;
import mapping.canonical.CanonicalElementType;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalNode;
import model.RDFPropertyInstance;

/**
 * Graph is the object that contains both logical and visual graph
 * Contains one Hashtables for nodes, one arraylist for edges and finally another Hashtable for TopK nodes
 * which enhances speed regarging TopK operations 
 * @author leonidis
 * 
 */
public class Graph {

//	private ArrayList<Edge> 		edgeList = null;
    private Hashtable<String, Edge> edgeList = null;
    private Hashtable<String, Node> nodeList = null;
    private Hashtable<String, Node> topKnodeList = null;
    private Hashtable<String, Node> starGnodeList = null;
    //If user activate top-K mode then this object array contains all nodes sorted in ascending order
    private Object[] topKnodes = null;
    //**************************************************************************************
    //The ranker that is used for the graph ranking
    private Ranker graphRanker = null;
    //**************************************************************************************
    //Variables used for determining the boundaries of drawing area (these portions are proportional to the total number of nodes in this graph)
    private int maxXrange = 0;
    private int maxYrange = 0;
    private int scaleFactor = 100;
    private int scaleCoordinateFactor = 100;
    //**************************************************************************************
    //Variables used in force-directed placement algorithm
    private double Ks = 1;				//Stiffness of spring
    private double Km = 50;				//Strength of magnetic field
    private double Ls = 150; 			//Length of the spring
    private double Ke = 500000;			//Repulsion Strength
    private int MaxIterations = 100;
    private int CheckStop = 0;
    //Variables used during moving nodes in force-directed placement algorithm
    private int maxXmove = 5;
    private int maxYmove = 5;
    private int Fthres = 10;
    //**************************************************************************************
    //Used in placements methods in order to determine which elements will be used (all or the top-K only)
    private boolean topKmode = false;
    //Each graph contains a reference to its visual graph
    private VisualGraph visGraph = null;
    //**************************************************************************************
    //Variables used in Dproperties
    private Color NodesColor = Color.GREEN;
    private int NodesWidth = 60;
    private int NodesHeigth = 20;
    private int NodesCurrentMaxWidth = 0;
    //**************************************************************************************
    //Namespaces that the Graph contains
    private ArrayList<String> nameSpaces;
    private DEPENDENCIES dependencyType;
    private Color namespaceColor;
    private Color defaultNsColors[] = {Color.YELLOW, Color.CYAN, Color.WHITE, Color.GREEN, Color.PINK, Color.MAGENTA};
    private Color instanceColor = Color.GRAY;
    //***************************************************************************************
    //Variable used for star graph browsing
    private boolean starGraphMode = false;
    private int starViewRadius = 1;
    private boolean viewIndirectEdges = false;
    //***************************************************************************************
    //Variable used to enable/disable smooth transitions
    private boolean smoothLayoutTransition = true;
    //Variable used to show the visibility of edges labels
    private boolean edgesLabelsVisibility = true;
    //Cache memory for fast accesing instances
    private InstanceCache iCache;
    /*NOT NEEDED NOW
    //Variable used to show the visibility of propertie edges
    private boolean propertieEdgesVisibility = true;
    //Variable used to show the visibility of isA edges
    private boolean isAEdgesVisibility = true;
     */
    //Watches the graph state
    private GraphState state;

    /**
     * Create a new Graph
     */
    public Graph() {
        nodeList = new Hashtable<String, Node>();
        topKnodeList = new Hashtable<String, Node>();
        starGnodeList = new Hashtable<String, Node>();

//		edgeList =  new ArrayList<Edge>();
        edgeList = new Hashtable<String, Edge>();
        //edgesFromNode = new Hashtable<Node, Hashtable<Node, ArrayList<Edge>>>();
        visGraph = new VisualGraph();
        nameSpaces = new ArrayList<String>();
        iCache = new InstanceCache(this, 1000);

        state = new GraphState(this);
    }//end Graph

    /**
     * Returns the list of edges of the graph
     * @return all edges of the graph in an arraylist
     */
    public Hashtable<String, Edge> getEdgeList() {
        return edgeList;
    }

    /**
     * Returns the list of nodes of the graph
     * @return all nodes in a hasttable
     */
    public Hashtable<String, Node> getNodeList() {
        return nodeList;
    }

    /**
     * Returns the state of the graph
     * @return the state of the graph
     */
    public GraphState getState() {
        return state;
    }

    /**
     * Returns the namespaces that the graph contains
     * @return an arrayList with the namespaces that the graph contains
     *
     */
    public ArrayList<String> getGraphNamespaces() {
        return nameSpaces;
    }

    /**
     * Create class nodes for all class elements in specified namespace
     * @param ns the namespace which would be visualized
     */
    private void createClassNodes(RDFNamespace ns) {
        Iterator it = ns.getClasses().iterator();
        while (it.hasNext()) {
            RDFClass classNode = (RDFClass) it.next();
            Node node = new Node(this, classNode, classNode.getLocalName(), 0, 0, 0, NodesWidth, NodesHeigth, ns.getURI(), SEMWEB_OBJECT_TYPE.CLASS);
            if (node.getGraphNode().getNodeWidth() > NodesCurrentMaxWidth) {
                NodesCurrentMaxWidth = (int) node.getGraphNode().getNodeWidth();
            }
            node.getGraphNode().setNodeGColor(namespaceColor);
            nodeList.put(node.getName(), node);
        }
    }

    /**
     * Create all data nodes loaded in the model
     * @param model - the model where the instances are loaded
     */
    private void createDataNodes(RDFModel model) {
        for (RDFResource r : model.getAllInstances()) {

            Node node = new Node(this, r, Utilities.extractLocalPartFromURI(r.toString()), 0, 0, 0, NodesWidth, NodesHeigth, r.getURI(), SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
            node.getGraphNode().setNodeGColor(instanceColor);
            node.setVisible(false);
            //If A Node is already constructed do not recreate
            ArrayList<Node> nodes = new ArrayList<Node>();
            ArrayList<Edge> edges = new ArrayList<Edge>();
            nodes.add(node);
            InstanceRecord ir = new InstanceRecord(node.getName(), nodes, edges);
            //We want to avoid adding a class node as an instance thats the reason
            //for the second clause
            if (!iCache.contains(node.getName()) && !nodeList.containsKey(node.getName())) {
                iCache.add(ir);

            }

        }

    }

    /**
     * Create class nodes for all class elements in specified namespace
     * @param ns the namespace which would be visualized
     */
    private void createIsaRelations(RDFModel model, RDFNamespace ns) {
        RDFClass rdf_rs = null;
        Iterator it = null;
        boolean isSubClass = false;


        //**************************************************************************************
        //Determine if this node is subclass and if so, connect it with its parent
        it = ns.getClasses().iterator();

        while (it.hasNext()) {
            rdf_rs = (RDFClass) it.next();
            if (rdf_rs.getSuperClasses().isEmpty()) {
                isSubClass = false;
            } else {
                isSubClass = true;
            }

            if (isSubClass) {
                int maxSuperClassLen = 0;
                if (dependencyType == DEPENDENCIES.DIRECT) {
                    maxSuperClassLen = 1;
                } else if (dependencyType == DEPENDENCIES.TRANSITIVE) {
                    maxSuperClassLen = -1;
                }
                createSuperHierarchies(rdf_rs, maxSuperClassLen);
            }//end if subClass
        }//end while -> classes

    }

    private void createDataRelations(RDFModel m) {
        for (Node classNode : nodeList.values()) {

            if (classNode.rdfObjectType == SEMWEB_OBJECT_TYPE.CLASS) {
                for (RDFResource r : m.getInstances(classNode.getName())) {
                    Node instanceNode = nodeList.get(Utilities.extractLocalPartFromURI(r.toString()));
                    //Create an edge between child and parent
                    Edge edge = new Edge(this, instanceNode, classNode, "instanceOF", SEMWEB_OBJECT_TYPE.INSTANCEOF, 0, true);
                    edgeList.put(edge.toString() + instanceNode.getName() + classNode.getName(), edge);
                    instanceNode.addEdgeFrom(edge);
                    //Add this edge to list of incoming edges of parent node
                    classNode.addEdgeTo(edge);
                }
            }
        }
    }

    /**
     * Given a name of a class it retrieves the instances if there are any
     * @param className - the name of the class that we want to retrive the instances
     * @return the class instances
     */
    public String[] getInstances(String className) {

        HashSet<String> instances = new HashSet<String>();
        //get instances from the model
        Node classNode = nodeList.get(className);
        for (Node ins : classNode.getInstances(false)) {
            instances.add(ins.getName());
        }


        //get instances from the swkm
        ProjectManager.getSingleton().getActiveProject().setActiveClient(classNode.getNodeNamespace());
        Client c = ProjectManager.getSingleton().getActiveProject().getActiveClient();

        //try to execute query to the client and populate instances
        //through swkm if a client exists
        if (c != null) {
            long time = System.currentTimeMillis();
            String ret = c.query().query(Format.RDF_XML, className);
            time -= time;
            System.out.println("Query getInstances Time=" + time + "ms ");

            long time2 = System.currentTimeMillis();
            instances.addAll(QueryUtils.list(ret, Format.RDF_XML));
            time2 -= time2;
            System.out.println("Query ADDALLgetInstances Time=" + time2 + "ms ");
        } else {
            System.out.println("The client is null!No connection to swkm exists");
        }


        return instances.toArray(new String[instances.size()]);
        //2 Different Options [Instances are already loaded - Instances must retrieved from SWKM]
        //For the first case search by className simple nodes
        //For the second case execute query
    }


    /**
     * Given an instance name if the current instance does not exist in the graph
     * create it along with his direct neighbours by executing queries to SWKM
     *
     * @param instanceName - the name of the instance
     * @param c - the client needed for executing the queries to SWKM
     */
    public InstanceRecord addInstanceAndNeighbours(String instanceName, Client c) {
         InstanceRecord ir = null;

        //If instance isn't loaded and the connection exists
        if (!iCache.contains(instanceName) && c != null) {

            //It is temporary dialog to show progress. Ideally it should  be replaced with
            //a progress bar
            JDialog tempd = new JDialog(MainFrame.getSingleton(), false);
            JLabel waitLabel = new JLabel("Please wait..\nInstance Retrieving");
            
            tempd.setLocation(MainFrame.getSingleton().getBounds().width/2,MainFrame.getSingleton().getBounds().height/2);
            tempd.add(waitLabel);
            tempd.pack();
            tempd.setVisible(true);
            tempd.update(tempd.getGraphics());
                

            //Query SWKM and retrieve instance and neigbours

            ArrayList<Node> irNodes = new ArrayList<Node>();
            ArrayList<Edge> irEdges = new ArrayList<Edge>();

            Node instanceNode = nodeList.get(instanceName);
            if (instanceNode == null) {
                instanceNode = new Node(this, null, instanceName, 0, 0, 0, NodesWidth, NodesHeigth, instanceName, SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
                instanceNode.getGraphNode().setNodeGColor(instanceColor);
            }

            irNodes.add(instanceNode);

            //Execute Query to connect instance with the appropriate classes

            String ret = c.query().query(Format.RDF_XML, "typeof(&" + instanceName + ")");

           
            ArrayList<String> classNames = new ArrayList(QueryUtils.list(ret, Format.RDF_XML));
            //Domain Properties
            ArrayList<String> Dproperties = new ArrayList<String>();
            //Range Properties
            ArrayList<String> Rproperties = new ArrayList<String>();

            for (String wName : classNames) {
                //Format the name of the class
                String cName = wName.split("#")[1];

                Edge instanceOFedge = null;


                Node classNode = nodeList.get(cName);
                if (classNode != null) {
                    //Connect the instance node with the class node

                    instanceOFedge = edgeList.get("InstanceOF" + instanceNode.getName() + classNode.getName());

                    if (instanceOFedge == null) {
                        instanceOFedge = new Edge(this, instanceNode, classNode, "InstanceOF", SEMWEB_OBJECT_TYPE.INSTANCEOF, 0, true);
                        classNode.removeEdgeTo(instanceOFedge);//We dont want duplicate edges
                        classNode.addEdgeTo(instanceOFedge);
                        instanceNode.removeEdgeFrom(instanceOFedge);//We dont want duplicate edges
                        instanceNode.addEdgeFrom(instanceOFedge);
                    }

                    irEdges.add(instanceOFedge);
                }

                //Execute 2 queries to retrieve all properties having as domain this class
                //and as range this class

                ret = c.query().query(Format.RDF_XML, "SELECT P FROM DProperty{P} WHERE domain(P) >= " + cName);
                Dproperties.addAll(QueryUtils.list(ret, Format.RDF_XML));
                ret = c.query().query(Format.RDF_XML, "SELECT P FROM DProperty{P} WHERE range(P) >= " + cName);
                Rproperties.addAll(QueryUtils.list(ret, Format.RDF_XML));
            }

            //For each domain property which results into the instance node do...
            for (String prop : Dproperties) {
                String fprop = prop.split("#")[1];

                ret = c.query().query(Format.RDF_XML, "SELECT Y from {X}^" + fprop + "{Y} where X like \"" + instanceName + "\"");
                ArrayList<String> nodesRetrieved = new ArrayList<String>(QueryUtils.list(ret, Format.RDF_XML));

                if (!nodesRetrieved.isEmpty()) {

                    for (String name : nodesRetrieved) {
                        Node n = nodeList.get(name);
                        if (n == null) {
                            n = new Node(this, null, name, 0, 0, 0, NodesWidth, NodesHeigth, name, SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
                            n.getGraphNode().setNodeGColor(instanceColor);

                        }
                        Edge e = edgeList.get(fprop + instanceNode.getName() + n.getName());
                        if (e == null) {
                            e = new Edge(this, instanceNode, n, fprop, SEMWEB_OBJECT_TYPE.PROPERTYINSTANCE, 0, true);

                            n.addEdgeTo(e);
                            instanceNode.addEdgeFrom(e);
                        }


                        irNodes.add(n);
                        irEdges.add(e);

                    }
                }

            }


            //For each range property which starts from the instance node do...

            for (String prop : Rproperties) {
                String fprop = prop.split("#")[1];

                ret = c.query().query(Format.RDF_XML, "SELECT X from {X}^" + fprop + "{Y} where Y like \"" + instanceName + "\"");

                ArrayList<String> nodesRetrieved = new ArrayList<String>(QueryUtils.list(ret, Format.RDF_XML));

                if (!nodesRetrieved.isEmpty()) {

                    for (String name : nodesRetrieved) {
                        Node n = nodeList.get(name);
                        if (n == null) {
                            n = new Node(this, null, name, 0, 0, 0, NodesWidth, NodesHeigth, name, SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
                            n.getGraphNode().setNodeGColor(instanceColor);

                        }
                        Edge e = edgeList.get(fprop + n.getName() + instanceNode.getName());
                        if (e == null) {
                            e = new Edge(this, n, instanceNode, fprop, SEMWEB_OBJECT_TYPE.PROPERTYINSTANCE, 0, true);

                            n.addEdgeFrom(e);
                            instanceNode.addEdgeTo(e);

                        }


                        irNodes.add(n);
                        irEdges.add(e);

                    }
                }
            }

            //Manual population for constants(String,Dates etc)
            if (irNodes.size() == 1 && irEdges.isEmpty()) {
                for (Edge e : instanceNode.getEdgesFrom()) {
                    irEdges.add(e);
                    irNodes.add(e.getTargetNode());
                }
                for (Edge e : instanceNode.getEdgesTo()) {
                    irEdges.add(e);
                    irNodes.add(e.getSourceNode());
                }
            }

            ir = new InstanceRecord(instanceName, irNodes, irEdges);

            //Add in cache
            iCache.add(ir);
            tempd.dispose();
        }

        return ir;

        
    }

//    /**
//     * A 2nd way in place of addInstanceAndNeighbours which sends less queries to the
//     * server but has the same execution time and for this reason is removed
//     *
//     * @param instanceName - the name of the instance
//     * @param c - the client needed for executing the queries to SWKM
//     */
//    public InstanceRecord addInstanceAndNeighbours2(String instanceName,Client c){
////        if(!iCache.contains(instanceName) ){//&& !nodeList.contains(instanceName)){
////        if(!nodeList.contains(instanceName)){
//        //If instance isn't loaded and the connection exists
//        InstanceRecord ir = null;
//        long timeQ1 = System.currentTimeMillis();
//
//
//        if(!iCache.contains(instanceName) && c != null){
//            //Query SWKM and retrieve instance and neigbours
//
//
//
//            ArrayList<Node> irNodes = new ArrayList<Node>();
//            ArrayList<Edge> irEdges = new ArrayList<Edge>();
//
//            Node instanceNode = nodeList.get(instanceName);
//            if(instanceNode == null){
//                instanceNode = new Node(this, null, instanceName, 0, 0, 0, NodesWidth, NodesHeigth, instanceName, SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
//                instanceNode.getGraphNode().setNodeGColor(instanceColor);
//            }else{
////                System.out.println("Node already created and used ="+instanceNode.getName());
//            }
//
//            irNodes.add(instanceNode);
//
//            //Execute Query to connect with appropriate class
//            String ret = c.query().query(Format.RDF_XML, "typeof(&"+instanceName+")");
//
//
//            ArrayList<String> classNames = new ArrayList(QueryUtils.list(ret, Format.RDF_XML));
//
//
//            for(String wName:classNames){
//
//                String cName = wName.split("#")[1];
////                System.out.println("ClassName= " + cName);
//                Edge instanceOFedge = null;
////                for (Edge e : instanceNode.getEdgesFrom()) {
////                    if (e.rdfObjectType == SEMWEB_OBJECT_TYPE.INSTANCEOF) {
////                        if (e.getTargetNode().getName().equals(cName)) {
////                            instanceOFedge = e;
////                        }
////                    }
////                }
//
//                Node classNode = nodeList.get(cName);
//                if (/*instanceOFedge == null &&*/ classNode != null) {
//                    //Create the connection
//
////                    instanceOFedge = new Edge(this, instanceNode, classNode, "InstanceOF", SEMWEB_OBJECT_TYPE.INSTANCEOF, 0, true);
////                    classNode.addEdgeTo(instanceOFedge);
////                    instanceNode.addEdgeFrom(instanceOFedge);
//
//                    instanceOFedge = edgeList.get("InstanceOF"+instanceNode.getName()+classNode.getName());
//
//                    if(instanceOFedge == null){
//                            instanceOFedge = new Edge(this, instanceNode, classNode, "InstanceOF", SEMWEB_OBJECT_TYPE.INSTANCEOF, 0, true);
//                            classNode.removeEdgeTo(instanceOFedge);//We dont want duplicate edges
//                            classNode.addEdgeTo(instanceOFedge);
//                            instanceNode.removeEdgeFrom(instanceOFedge);//We dont want duplicate edges
////                            System.out.println("RemoveEdge:"+instanceOFedge);
//                            instanceNode.addEdgeFrom(instanceOFedge);
//                    }else{
////                            System.out.println("Edge already created and used ="+instanceOFedge.toString());
//
//                    }
//
//
//                }
//
//
//
//                if(classNode != null){
//
//                    irEdges.add(instanceOFedge);
//
//
//                }
//
//
//                    ret = c.query().query(Format.RDF_XML, "SELECT Y,@P from {X}^@P{Y} where X like \""+instanceName+"\"");
//                    List<List<String>> nodesNProps1 = QueryUtils.listOfMultiple(ret, Format.RDF_XML);
//                    ret = c.query().query(Format.RDF_XML, "SELECT Χ,@P from {X}^@P{Y} where Υ like \""+instanceName+"\"");
//                    List<List<String>> nodesNProps2 = QueryUtils.listOfMultiple(ret, Format.RDF_XML);
//
//                    for(List<String> s:nodesNProps1){
//
//                         String nodeN = s.get(0);
//                          String edgeN = Utilities.extractLocalPartFromURI(s.get(1));
//                          Node n = nodeList.get(nodeN);
//                        if(n ==null){
//                            n = new Node(this, null, nodeN, 0, 0, 0, NodesWidth, NodesHeigth, nodeN,SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
//                            n.getGraphNode().setNodeGColor(instanceColor);
//
//                        }else{
////                            System.out.println("Node already created and used ="+n.getName());
//                        }
//                        Edge e = edgeList.get(edgeN+instanceNode.getName()+n.getName());
//                        if(e == null){
//                            e = new Edge(this, instanceNode, n, edgeN, SEMWEB_OBJECT_TYPE.PROPERTYINSTANCE, 0, true);
//
////                            n.getEdgesFrom().clear();
////                            n.getEdgesTo().clear();
//
//                            n.addEdgeTo(e);
//                            instanceNode.addEdgeFrom(e);
//                        }else{
////                            System.out.println("Edge already created and used ="+e.toString());
//                        }
//
//
//                        irNodes.add(n);
//                        irEdges.add(e);
//
//
//                     }
//                    for(List<String> s:nodesNProps2){
//
//                         String nodeN = s.get(0);
//                          String edgeN = Utilities.extractLocalPartFromURI(s.get(1));
//                          Node n = nodeList.get(nodeN);
//                        if(n ==null){
//                            n = new Node(this, null, nodeN, 0, 0, 0, NodesWidth, NodesHeigth, nodeN,SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
//                            n.getGraphNode().setNodeGColor(instanceColor);
//
//                        }else{
////                            System.out.println("Node already created and used ="+n.getName());
//                        }
//                        Edge e = edgeList.get(edgeN+instanceNode.getName()+n.getName());
//                        if(e == null){
//                            e = new Edge(this, instanceNode, n, edgeN, SEMWEB_OBJECT_TYPE.PROPERTYINSTANCE, 0, true);
//
////                            n.getEdgesFrom().clear();
////                            n.getEdgesTo().clear();
//
//                            n.addEdgeFrom(e);
//                            instanceNode.addEdgeFrom(e);
//                        }else{
////                            System.out.println("Edge already created and used ="+e.toString());
//                        }
//
//
//                        irNodes.add(n);
//                        irEdges.add(e);
//
//
//                     }
//            }
//
//
//            //Manual population for constants(String,Dates etc)
//            if(irNodes.size()==1 && irEdges.size()==0){
//                for(Edge e:instanceNode.getEdgesFrom()){
//                    irEdges.add(e);
//                    irNodes.add(e.getTargetNode());
//                }
//                for(Edge e:instanceNode.getEdgesTo()){
//                    irEdges.add(e);
//                    irNodes.add(e.getSourceNode());
//                }
//            }
//
//            ir = new InstanceRecord(instanceName, irNodes, irEdges);
//
//
//            //Add in cache
//            iCache.add(ir);
//        }
//
//        timeQ1 -= System.currentTimeMillis();
//        System.out.println("ADDinstanceNeighbours M2 time="+timeQ1+" ms");
//        return ir;
//    }

    private void createSuperHierarchies(RDFClass cl, int levels) {
        Iterator superClassIt = cl.getSuperClasses().iterator();
        Node currentNode = nodeList.get(cl.getLocalName());
        while (superClassIt.hasNext()) {
            RDFClass parent = (RDFClass) superClassIt.next();
            Node parentNode = nodeList.get(parent.getLocalName());

            //If parent node == null then there is an external dependency
            if (parentNode == null) {
                //If levels != 0 then create this external dependency
                if (levels == 0) {
                    continue;
                }
                String namespace = extractNamespace(parent.getURI());
                if (!nameSpaces.contains(namespace)) {
                    nameSpaces.add(namespace);
                }
                parentNode = new Node(this, parent, parent.getLocalName(), 0, 0, 0, NodesWidth, NodesHeigth, namespace, SEMWEB_OBJECT_TYPE.CLASS);
                //Caclulate the maximun width of all nodes
                if (parentNode.getGraphNode().getNodeWidth() > NodesCurrentMaxWidth) {
                    NodesCurrentMaxWidth = (int) parentNode.getGraphNode().getNodeWidth();
                }
                parentNode.getGraphNode().setNodeGColor(NodesColor.brighter());
                nodeList.put(parentNode.getName(), parentNode);
            }
            //We want to avoid cycles of isA edges so we dont want the same target
            //and destination for isA edges
            if(currentNode.getName().equals(parentNode.getName())){
                //If they have the same name they are the same node and we do not
                //want to create this types of edges skip to the next
                continue;
            }
            String edgeKeyCreate = "isA" + currentNode.getName() + parentNode.getName();
            Edge e = edgeList.get(edgeKeyCreate);
            if (e == null) {
                e = new Edge(this, currentNode, parentNode, "isA", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
                edgeList.put(edgeKeyCreate, e);
                currentNode.addEdgeFrom(e);
                parentNode.addEdgeTo(e);
                System.out.println("EDGE PUT=" + e + ":" + currentNode.getName() + ":" + parentNode.getName());
            }


            //Create external dependencies recursively with max recursion level
            if (!parent.getAllSuperClasses().isEmpty() && levels != 1 && levels != 0) {
                System.out.println("Recursion because of:" + currentNode.getName());
                createSuperHierarchies(parent, levels - 1);
            }
        }

    }

    private String extractNamespace(String uri) {
        int end = uri.lastIndexOf("#");
        return uri.substring(0, end);
    }
   

    /**
     * Create property nodes for all class elements in specified namespace
     * @param ns the namespace which would be visualized
     */
    private void createPropertiesElements(RDFModel model, RDFNamespace ns) {
        Iterator it, rangeIt, domainIt;
        RDFResource rdf_rs, range, domain;
        Node domainNode;
        Node rangeNode;

        //Variable used when a property has domain or range a class outside of specified namespace
        //and a new node must be created
        boolean newNode = false;
        //Variable used to represent the total number of same edges that connect 2 nodes
        int nedges = 0;

        it = ns.getProperties().iterator();
        while (it.hasNext()) {
            rdf_rs = (RDFProperty) it.next();

            //Get all ranges of this property
            rangeIt = ((RDFProperty) rdf_rs).getRanges().iterator();
            //If iterator is null then this property has no range classes, continue with another
            if (!rangeIt.hasNext()) {
                continue;
            }

            range = (RDFClass) rangeIt.next();
            //Check if this node already exists(created during nodes generations) or has to be created
            if (nodeList.containsKey(range.getLocalName())) {
                rangeNode = nodeList.get(range.getLocalName());
            } else {
                if (dependencyType == DEPENDENCIES.DIRECT) {
                    //RDFNamespace namespace = model.getNamespace(range.getURI());
                    String rnNamespace = extractNamespace(range.getURI());
                    if (!nameSpaces.contains(rnNamespace)) {
                        nameSpaces.add(rnNamespace);
                    }
                    rangeNode = new Node(this, range, range.getLocalName(), 0, 0, 0, NodesWidth + 20, NodesHeigth, rnNamespace, SEMWEB_OBJECT_TYPE.CLASS);
                    rangeNode.getGraphNode().setNodeGColor(NodesColor.brighter());
                    newNode = true;
                    nodeList.put(rangeNode.getName(), rangeNode);
                } else {
                    continue;
                }
                //visGraph.addNode(rangeNode);
            }

            //Get all domains of this property
            domainIt = ((RDFProperty) rdf_rs).getDomains().iterator();
            //If iterator is null then this property has no domain classes, continue with another
            if (!domainIt.hasNext()) {
                continue;
            }
            domain = (RDFClass) domainIt.next();
            //Check if this node already exists(created during nodes generations) or has to be created
            if (nodeList.containsKey(domain.getLocalName())) {
                domainNode = nodeList.get(domain.getLocalName());
            } else {
                if (dependencyType == DEPENDENCIES.DIRECT) {
                    String dnNamespace = extractNamespace(range.getURI());
                    if (!nameSpaces.contains(dnNamespace)) {
                        nameSpaces.add(dnNamespace);
                    }
                    domainNode = new Node(this, domain, domain.getLocalName(), 0, 0, 0, NodesWidth + 20, NodesHeigth, dnNamespace, SEMWEB_OBJECT_TYPE.CLASS);
                    domainNode.getGraphNode().setNodeGColor(NodesColor.brighter());
                    newNode = true;
                    nodeList.put(domainNode.getName(), domainNode);
                } else {
                    continue;
                }
            }

            nedges = 0;
            //If domain/range node is a new node there won't be any edges connecting it with another node
            if (!newNode) {
                nedges = domainNode.getEdgesNoToNode(rangeNode);
            }

            //Create edge and add it to
            //1. domain node edgeFrom list
            //2. range node edgeTo list
            Edge edge = new Edge(this, domainNode, rangeNode, rdf_rs.getLocalName(), SEMWEB_OBJECT_TYPE.PROPERTY, nedges, true);
            edge.setSubProperties(((RDFProperty) rdf_rs).getDirectDescendants());

            edgeList.put(edge + domainNode.getName() + rangeNode.getName(), edge);
            domainNode.addEdgeFrom(edge);
            rangeNode.addEdgeTo(edge);

            newNode = false;
        }//end while -> Dproperties
    }//end createPropertiesElements

    private void createPropertieInstances(RDFModel model) {
        for (RDFPropertyInstance pr_in : model.getAllPropertyInstances()) {

            Node domainNode = null;
            String domainNodeName = Utilities.extractLocalPartFromURI(pr_in.getSubject().toString());
            if (iCache.contains(domainNodeName)) {
                domainNode = nodeList.get(domainNodeName);

            } else {

                domainNode = new Node(this, pr_in.getSubject(), domainNodeName, 0, 0, 0, NodesWidth + 20, NodesHeigth, pr_in.getSubject().toString(), SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
                domainNode.getGraphNode().setNodeGColor(instanceColor);
                domainNode.setVisible(false);
                ArrayList<Node> nodes = new ArrayList<Node>();
                ArrayList<Edge> edges = new ArrayList<Edge>();
                nodes.add(domainNode);
                InstanceRecord ir = new InstanceRecord(domainNode.getName(), nodes, edges);
                iCache.add(ir);
            }
            Node rangeNode = null;
            String rangeNodeName = Utilities.extractLocalPartFromURI(pr_in.getObject().toString());
            if (iCache.contains(rangeNodeName)) {
                rangeNode = nodeList.get(rangeNodeName);

            } else {
                rangeNode = new Node(this, null, rangeNodeName, 0, 0, 0, NodesWidth + 20, NodesHeigth, pr_in.getObject().toString(), SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
                rangeNode.getGraphNode().setNodeGColor(instanceColor);
                rangeNode.setVisible(false);
                ArrayList<Node> nodes = new ArrayList<Node>();
                ArrayList<Edge> edges = new ArrayList<Edge>();
                nodes.add(rangeNode);
                InstanceRecord ir = new InstanceRecord(rangeNode.getName(), nodes, edges);
                iCache.add(ir);

            }

            Edge edge = new Edge(this, domainNode, rangeNode, pr_in.getPredicate().getLocalName(), SEMWEB_OBJECT_TYPE.PROPERTYINSTANCE, 0, true);
            domainNode.addEdgeFrom(edge);
            rangeNode.addEdgeTo(edge);
            edgeList.put(edge + domainNode.getName() + rangeNode.getName(), edge);




        }
    }

    /**
     * Populate the Graph using a given RDF_Model
     * Firstly create all class nodes and create isA edges
     * Then create all model Dproperties and connect their domain / range
     * @param model : RDF_Model that will be used in graph population
     * @param namespace_uri the chosen namespace
     */
    public void populateGraph(RDFModel model/*, String namespace_uri*/) { // load dependent nss entirely
        //We use this copy of nameSpaces because due to direct and
        //transitive dependencies we might add more namespaces in the list
        ArrayList<String> nSpaces = new ArrayList<String>(nameSpaces);
        int colorCounter = 0;
        for (String nn : nSpaces) {
            //Select the colour for the namespace
            if (colorCounter < defaultNsColors.length) {
                namespaceColor = defaultNsColors[colorCounter];
                colorCounter++;
            } else {
                namespaceColor = generateNameSpaceColor();
            }
            RDFNamespace rdfnn = model.getNamespace(nn);
            if(rdfnn != null){
                createClassNodes(rdfnn);
            }

        }
        for (String nn : nSpaces) {
            RDFNamespace rdfnn = model.getNamespace(nn);
            if(rdfnn != null){
                createIsaRelations(model,rdfnn);
                createPropertiesElements(model, rdfnn);
            }
            
        }
        createDataNodes(model);
        createDataRelations(model);
        createPropertieInstances(model);

        System.out.println("Namespaces contained = ");
        Iterator it = nameSpaces.iterator();
        while (it.hasNext()) {
            String test = (String) it.next();
            System.out.println(test);
        }
        maxXrange = nodeList.size() * 120;
        maxYrange = nodeList.size() * 50;

    }//end populateGraph

    /**
     * Populate graph directly from canonical snapshot data.
     * This is used by the Jena-first visualization path where SWKM model objects
     * may be intentionally absent.
     *
     * @param snapshot canonical graph snapshot
     */
    public void populateGraph(CanonicalGraphSnapshot snapshot) {
        ArrayList<String> selectedNamespaces = new ArrayList<String>(nameSpaces);
        nodeList.clear();
        edgeList.clear();
        topKnodeList.clear();
        starGnodeList.clear();
        iCache.clear();

        if (snapshot == null) {
            nameSpaces.clear();
            maxXrange = 0;
            maxYrange = 0;
            return;
        }

        ArrayList<String> canonicalNamespaces = new ArrayList<String>(snapshot.getNamespaces());
        nameSpaces.clear();
        if (selectedNamespaces.isEmpty()) {
            nameSpaces.addAll(canonicalNamespaces);
        } else {
            nameSpaces.addAll(selectedNamespaces);
        }
        HashSet<String> activeNamespaces = new HashSet<String>(nameSpaces);

        HashMap<String, Color> namespaceColors = new HashMap<String, Color>();
        int colorCounter = 0;

        HashMap<String, Node> nodeById = new HashMap<String, Node>();
        colorCounter = appendCanonicalNodes(snapshot.getNodes(), activeNamespaces, true, namespaceColors, colorCounter, nodeById);
        if (nodeList.isEmpty() && !snapshot.getNodes().isEmpty() && !activeNamespaces.isEmpty()) {
            // If namespace matching removes every node, fallback to unfiltered canonical nodes
            // so valid graphs are still visible when namespace strings differ in formatting.
            nodeList.clear();
            nodeById.clear();
            namespaceColors.clear();
            colorCounter = 0;
            colorCounter = appendCanonicalNodes(snapshot.getNodes(), activeNamespaces, false, namespaceColors, colorCounter, nodeById);
        }

        for (CanonicalEdge canonicalEdge : snapshot.getEdges()) {
            Node source = nodeById.get(canonicalEdge.getSourceId());
            Node target = nodeById.get(canonicalEdge.getTargetId());
            if (source == null || target == null) {
                continue;
            }

            SEMWEB_OBJECT_TYPE edgeType = toGraphEdgeType(canonicalEdge.getType());
            String label = canonicalEdge.getLabel() == null ? "" : canonicalEdge.getLabel();
            int edgeIndex = source.getEdgesNoToNode(target);
            Edge edge = new Edge(this, source, target, label, edgeType, edgeIndex, canonicalEdge.isDirected());
            edge.setVisible(canonicalEdge.isVisible());
            source.addEdgeFrom(edge);
            target.addEdgeTo(edge);
            edgeList.put(canonicalEdge.getId() + source.getName() + target.getName(), edge);
        }

        maxXrange = nodeList.size() * 120;
        maxYrange = nodeList.size() * 50;
    }

    private int appendCanonicalNodes(
            List<CanonicalNode> canonicalNodes,
            Set<String> activeNamespaces,
            boolean enforceNamespaceFilter,
            Map<String, Color> namespaceColors,
            int colorCounter,
            Map<String, Node> nodeById
    ) {
        for (CanonicalNode canonicalNode : canonicalNodes) {
            String nodeName = canonicalNode.getName();
            if (nodeName == null || nodeName.trim().isEmpty()) {
                nodeName = Utilities.extractLocalPartFromURI(canonicalNode.getUri());
            }
            if (nodeName == null || nodeName.trim().isEmpty()) {
                nodeName = canonicalNode.getId();
            }
            if (nodeName == null || nodeName.trim().isEmpty()) {
                continue;
            }

            String namespace = canonicalNode.getNamespace();
            if (namespace == null || namespace.trim().isEmpty()) {
                namespace = "";
            }
            if (enforceNamespaceFilter && !activeNamespaces.isEmpty() && !activeNamespaces.contains(namespace)) {
                continue;
            }

            Color color = namespaceColors.get(namespace);
            if (color == null) {
                if (colorCounter < defaultNsColors.length) {
                    color = defaultNsColors[colorCounter];
                } else {
                    color = generateNameSpaceColor();
                }
                namespaceColors.put(namespace, color);
                colorCounter++;
            }

            SEMWEB_OBJECT_TYPE nodeType = canonicalNode.getType() == CanonicalElementType.CLASS_INSTANCE
                    ? SEMWEB_OBJECT_TYPE.CLASSINSTANCE
                    : SEMWEB_OBJECT_TYPE.CLASS;
            Node node = new Node(this, null, nodeName, 0, 0, 0, NodesWidth, NodesHeigth, namespace, nodeType);
            node.getGraphNode().setNodeGColor(nodeType == SEMWEB_OBJECT_TYPE.CLASSINSTANCE ? instanceColor : color);
            node.setVisible(canonicalNode.isVisible());
            node.setNailed(canonicalNode.isNailed());
            if (node.getGraphNode().getNodeWidth() > NodesCurrentMaxWidth) {
                NodesCurrentMaxWidth = (int) node.getGraphNode().getNodeWidth();
            }

            nodeList.put(node.getName(), node);
            if (canonicalNode.getId() != null) {
                nodeById.put(canonicalNode.getId(), node);
            }
            nodeById.put(node.getName(), node);
        }
        return colorCounter;
    }

    private SEMWEB_OBJECT_TYPE toGraphEdgeType(CanonicalElementType type) {
        if (type == null) {
            return SEMWEB_OBJECT_TYPE.PROPERTY;
        }
        switch (type) {
            case SUBCLASS_OF:
                return SEMWEB_OBJECT_TYPE.SUBCLASSOF;
            case INSTANCE_OF:
                return SEMWEB_OBJECT_TYPE.INSTANCEOF;
            case PROPERTY_INSTANCE:
                return SEMWEB_OBJECT_TYPE.PROPERTYINSTANCE;
            case PROPERTY:
            case UNKNOWN:
            default:
                return SEMWEB_OBJECT_TYPE.PROPERTY;
        }
    }

    //TODO Rethink of this method for making a parser
    public void populateGraph(InputStream inputStream,String streamURI) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        InputStreamReader isReader = new InputStreamReader(bis);
        BufferedReader bReader = new BufferedReader(isReader);
        boolean directedGraph = true;

        //Find an appropriate colour for the nodes
        int colorCounter = 0;
        for(String ns:nameSpaces){
            if(ns.equals(streamURI)){
                break;
            }
            colorCounter++;
        }
        if (colorCounter < defaultNsColors.length) {
            namespaceColor = defaultNsColors[colorCounter];
        } else {
            namespaceColor = generateNameSpaceColor();
        }


        String graphStyle = bReader.readLine();
        if (graphStyle != null) {
            if (graphStyle.equals("GraphStyle:Directed")) {
                directedGraph = true;
            } else if (graphStyle.equals("GraphStyle:NotDirected")) {
                directedGraph = false;
            } else {
                //TODO Add an exception of some type
            }
            String readLine = bReader.readLine();
            int line = 0;
            while (readLine != null) {
                //Create a tokenizer to split the input line
                StringTokenizer sTokenizer = new StringTokenizer(readLine);

                int tokenNum = sTokenizer.countTokens();
                String tokens[] = new String[tokenNum];
                //Put the tokens in the token array
                for (int j = 0; j < tokenNum; j++) {
                    tokens[j] = sTokenizer.nextToken();
                }
                Node nodeA;
                Node nodeB;

                switch (tokenNum) {
                    case 0:
                        //Just ignore empty new lines \n
                        break;
                    case 1:
                        if (!nodeList.containsKey(tokens[0])) {

                            nodeA = new Node(this, null, tokens[0], 0, 0, 0, NodesWidth, NodesHeigth, streamURI, SEMWEB_OBJECT_TYPE.CLASS);
                            nodeA.getGraphNode().setNodeGColor(namespaceColor);
                            nodeList.put(nodeA.getName(), nodeA);
                        }
                        //Create one node
                        break;
                    case 2:
                        //Create two nodes connected with an unamed edge
                        nodeA = nodeList.get(tokens[0]);
                        nodeB = nodeList.get(tokens[1]);
                        if (nodeA == null) {
                            //Create a new nodeA

                            nodeA = new Node(this, null, tokens[0], 0, 0, 0, NodesWidth, NodesHeigth, streamURI, SEMWEB_OBJECT_TYPE.CLASS);
                            nodeA.getGraphNode().setNodeGColor(namespaceColor);
                            nodeList.put(nodeA.getName(), nodeA);
                        }
                        if (nodeB == null) {
                            //Create a new nodeA

                            nodeB = new Node(this, null, tokens[1], 0, 0, 0, NodesWidth, NodesHeigth, streamURI, SEMWEB_OBJECT_TYPE.CLASS);
                            nodeB.getGraphNode().setNodeGColor(namespaceColor);
                            nodeList.put(nodeB.getName(), nodeB);
                        }


                        break;
                    case 3:
                        //Create two nodes connected with a named edge
                        nodeA = nodeList.get(tokens[0]);
                        nodeB = nodeList.get(tokens[1]);
                        System.out.println("TOKEN0:" + tokens[0] + "TOKEN1:" + tokens[1]);
                        if (nodeA == null) {
                            //Create a new nodeA

                            nodeA = new Node(this, null, tokens[0], 0, 0, 0, NodesWidth, NodesHeigth, streamURI, SEMWEB_OBJECT_TYPE.CLASS);
                            nodeA.getGraphNode().setNodeGColor(namespaceColor);
                            nodeList.put(nodeA.getName(), nodeA);
                        }
                        if (nodeB == null) {
                            //Create a new nodeA

                            nodeB = new Node(this, null, tokens[1], 0, 0, 0, NodesWidth, NodesHeigth, streamURI, SEMWEB_OBJECT_TYPE.CLASS);
                            nodeB.getGraphNode().setNodeGColor(namespaceColor);
                            nodeList.put(nodeB.getName(), nodeB);
                        }
                        Edge edge;
                        if(tokens[2].equalsIgnoreCase("isa")){
                            //Create an isa edge
                            edge = new Edge(this, nodeA, nodeB, tokens[2], SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, directedGraph);
                        }else{
                            //Create a simple property edge
                            edge = new Edge(this, nodeA, nodeB, tokens[2], SEMWEB_OBJECT_TYPE.PROPERTY, 0, directedGraph);
                        }
                        
                        nodeA.addEdgeFrom(edge);
                        nodeB.addEdgeTo(edge);

                        edgeList.put(edge + nodeA.getName() + nodeB.getName(), edge);
                        break;
                    default:
                    //TODO Add an proper error 
                    }

                readLine = bReader.readLine();
                System.out.println(line++);
            }
            System.out.println("NODES NUM:" + nodeList.size());
            maxXrange = nodeList.size() * 120;
            maxYrange = nodeList.size() * 50;
        }

    }

    /**
     * Creates the ranker for the Top-K diagrams and adds it to the graph
     */
    public void createGraphRanker() {
        Ranker ranker = new Ranker(nodeList.values(), edgeList.values());
        graphRanker = ranker;
    }

    /**
     * Gets the current ranker that is used for this graph
     * @return the ranker of the current graph
     */
    public Ranker getGraphRanker() {
        return graphRanker;
    }


    /**
     *	Set nodes x and y positions of this graph using a random placement algorithm
     */
    public boolean randomLayout() {
        arrangeDisconectedNodes();
        //Variables used for placing nodes in the canvas "center"
        int maxXportion = maxXrange / 3;
        int maxYportion = maxYrange / 3;
        //Temporal node
        Node n = null;
        //Objects used for random coordinates generation
        Date d = new Date();
        Random r = new Random(d.getTime());
        Iterator it = nodeList.values().iterator();

        int x, y;
        //Iterate all nodes and place them in random positions
        while (it.hasNext()) {
            n = (Node) it.next();
            //If node is nailed or invisible do not move it
            if (n.nailed || !n.visible) {
                continue;
            }

            x = r.nextInt(maxXportion);
            y = r.nextInt(maxYportion);
            n.setX(x);
            n.setY(y);
        }//end while hasNext
        //Patch for reseting visibility of invisible nodes after random placement
        makeNodesInvisible();
        return true;
    }//end randomLayout

    /**
     * Arranges all the nodes with no edges in the top left corner of the scree and nail's them
     * so that aren't moved by FDPA
     *
     */
    public void arrangeDisconectedNodes() {
        Iterator it = nodeList.values().iterator();
        Node n = null;
        int offsetX = 10;
        int offsetY = 10;

        while (it.hasNext()) {
            n = (Node) it.next();
            int totalEdges = n.getTotalEdges();
            if (totalEdges == 0) {
                n.setX(offsetX);
                n.setY(offsetY);
                n.nailed = true;
                offsetY += n.getGraphNode().getNodeHeigth();
                /* If the Y offset becomes very big and exceeds a typical 800x600 resolution then
                 * we start making steps in X and with that way is almost impossible to run out of screen space
                 */
                if (offsetY > 800) {
                    offsetX += NodesCurrentMaxWidth;
                    offsetY = 10;
                }
            }
        }

    }

    

    public ArrayList<String> topKNodes(String params, Point centerOfWindow) {
        int rank = 0;
        int visibleNo = 0;
        int tmpRank = 0;
        double score = 0.0;
        int j = 0;
        //long start = System.currentTimeMillis();
        ArrayList<String> topKnodesNames = new ArrayList<String>();

        try {
            rank = Integer.valueOf(params);
            System.out.println("\nUser requested Top - " + rank + " nodes.");
        } catch (NumberFormatException NFe) {
            return null;
        }

        activateTopK();
        topKnodeList.clear();

        for (int i = topKnodes.length - 1; i >= 0 && j < rank; i--, j++) {
            visibleNo++;
            ((Node) topKnodes[i]).setVisible(true);
            topKnodeList.put(((Node) topKnodes[i]).getName(), (Node) topKnodes[i]);
            topKnodesNames.add(((Node) topKnodes[i]).getName());
        }//end for i<topKnodes.length

        //All nodes left must become invisible
        for (int i = 0; i < topKnodes.length - visibleNo; i++) //((Node)topKnodes[i]).setVisible(false);
        {
            ((Node) topKnodes[i]).visible = false;
        }

        //Iterate top-K visible nodes in order to create their top-K visible edges (those connected
        //with visible nodes). Patch used for speeding-up some layout algorithms
        Iterator topKnodeIt = topKnodeList.values().iterator();
        Iterator edgesFromIt = null, edgesToIt = null;
        Node n, tmpNode;
        Edge edge;

        //For each node, get its incoming and outgoing edges.
        //For each edge check if its target or source node respectively is visible
        //and if so add this edge to the appropriate topKedges list
        while (topKnodeIt.hasNext()) {
            n = (Node) topKnodeIt.next();
            n.getSpeedEdgesFrom().clear();
            n.getSpeedEdgesTo().clear();
            edgesFromIt = n.getEdgesFrom().iterator();
            edgesToIt = n.getEdgesTo().iterator();

            while (edgesFromIt.hasNext()) {
                edge = (Edge) edgesFromIt.next();
                tmpNode = edge.getTargetNode();
                if (tmpNode.visible) {
                    n.addSpeedEdgeFrom(edge);
                }
            }//end while edgesFromIt.hasNext

            while (edgesToIt.hasNext()) {
                edge = (Edge) edgesToIt.next();
                tmpNode = edge.getSourceNode();
                if (tmpNode.visible) {
                    n.addSpeedEdgeTo(edge);
                }
            }//end while edgesToIt.hasNext
        }//end while topKnodeIt.hasNext

        for (int i = 0; i < topKnodesNames.size(); i++) {
            System.out.println(topKnodesNames.get(i));
        }
        return topKnodesNames;
    }//end topKNnodes

    /**
     * Make invisible all nodes of this graph, whose rank is lower than given rank
     * @param params : lower rank
     */
    public ArrayList<String> topKGroups(String params) {
        int rank = 0;
        int visibleNo = 0;
        int tmpRank = 0;
        double score = 0.0;

        ArrayList<String> topKnodesNames = new ArrayList<String>();

        try {
            rank = Integer.valueOf(params);
            System.out.println("\nUser requested Top - " + rank + " groups.");
        } catch (NumberFormatException NFe) {
            return null;
        }

        activateTopK();
        topKnodeList.clear();

        //Top-K nodes must be added to topKnodeList (so as to be used later in layout algorithms).
        //The for-loop condition is either no nodes were left or (top-K + same-ranked) nodes were
        //made visible.
        //Same-ranked : if the user requests top-10 and 10th,11th,12th have the same rank
        //then user will see top-12 indeed
        ////////////////////////////////////////////////////////////////////////////////////////
        for (int i = topKnodes.length - 1; i >= 0; i--) {

            if (((Node) topKnodes[i]).rankingScore != score) {
                score = ((Node) topKnodes[i]).rankingScore;
                tmpRank++;
            }
            if (tmpRank == rank + 1) {
                break;
            }

            visibleNo++;
            ((Node) topKnodes[i]).setVisible(true);
            topKnodeList.put(((Node) topKnodes[i]).getName(), (Node) topKnodes[i]);
            topKnodesNames.add(((Node) topKnodes[i]).getName());
        }//end for i<topKnodes.length

        //All nodes left must become invisible
        for (int i = 0; i < topKnodes.length - visibleNo; i++) {
            ((Node) topKnodes[i]).setVisible(false);
        }

        completeEdgeSpeedList(topKnodeList);

        for (int i = 0; i < topKnodesNames.size(); i++) {
            System.out.println(topKnodesNames.get(i));
        }

        return topKnodesNames;
    }//end topKGroups

    /**
     * Dispatch ranking method accoring to its method identifier
     * @param method chosen ranking method identifier
     * @param params params used by chosen ranking method
     * @param model opened model
     * @param namespace_uri chosen namespace
     * @return hashtable with connectivity informations
     */
    public Hashtable<String, ConnectivityInfo> rank(String method, String params, String namespace_uri) {

        Hashtable<String, ConnectivityInfo> objs = null;
        double q1 = graphRanker.getQ1();
        double q2 = graphRanker.getQ2();
        double q3 = graphRanker.getQ3();

        if (!params.equals("")) {
            q1 = Double.parseDouble(params.split(",")[0]);
            q2 = Double.parseDouble(params.split(",")[1]);
            q3 = Double.parseDouble(params.split(",")[2]);

            if (q1 + q2 + q3 != 1) {
                return objs;
            }
        }

        if ("method0".equals(method)) {
            objs = graphRanker.method0(namespace_uri);
        } else if ("method2".equals(method)) {
            objs = graphRanker.method2(namespace_uri, q1, q2, q3);
        } else if ("method5".equals(method)) {
            objs = graphRanker.method5(namespace_uri);
        }

        topKnodes = graphRanker.getRankedNodes();
        return objs;
    }//end rank

    /**
     * Return the Euclideian Distance between two points
     * @param p first point
     * @param q last point
     * @return  distance
     */
    private double calculateDistance(Point p, Point q) {
        double dist = 0;

        int dx = p.x - q.x;
        int dy = p.y - q.y;
        dist = Math.sqrt((double) (Math.pow(dx, 2) + Math.pow(dy, 2)));

        return dist;
    }//end calculateDistance

    /**
     * Parses the string that contains all parameters needed in Force-Directed Placement Algorithm and
     * initialize the appropriate class members
     * @param params
     * @return true if parsing was successful, false otherwise
     */
    public boolean parseForceParameters(String params) {
        try {
            Ks = Double.parseDouble(params.split(",")[0]);
            Ls = Double.parseDouble(params.split(",")[1]);
            Ke = Double.parseDouble(params.split(",")[2]);
            Km = Double.parseDouble(params.split(",")[3]);
            MaxIterations = Integer.parseInt(params.split(",")[4]);
            CheckStop = Integer.parseInt(params.split(",")[5]);
            maxXmove = Integer.parseInt(params.split(",")[6]);
            maxYmove = Integer.parseInt(params.split(",")[7]);
            Fthres = Integer.parseInt(params.split(",")[8]);
            return true;
        }//end try
        catch (NumberFormatException e) {
            return false;
        }
    }//end parseForceParameters

    /**
     * Change one of the FDPA parameters.The parameter to be changed is specified by paramToChange and the new value of the
     * parameter is specified by the value variable
     * @param paramToChange - the name of the parameter of the FDPA to be changed
     * @param value - the new value of the parameter
     */
    public void changeFDPAparam(String paramToChange, double value) {
        if (paramToChange.equals("Ks")) {
            Ks = value;
        } else if (paramToChange.equals("Ls")) {
            Ls = value;
        } else if (paramToChange.equals("Ke")) {
            setKe(value);
        } else if (paramToChange.equals("Km")) {
            setKm(value);
        } else if (paramToChange.equals("MaxIterations")) {
            MaxIterations = (int) value;
        } else if (paramToChange.equals("CheckStop")) {
            CheckStop = (int) value;
        } else if (paramToChange.equals("maxXmove")) {
            maxXmove = (int) value;
        } else if (paramToChange.equals("maxYmove")) {
            maxYmove = (int) value;
        } else if (paramToChange.equals("Fthres")) {
            Fthres = (int) value;
        }
    }

    /**
     * Compute all forces needed by Force-Directed Placement Algorithm using this formula
     * @param nodeList list with all graph nodes
     * @param iteration
     * @return true if computation was successful, false otherwise
     */
    public boolean computeForces(Hashtable<String, Node> nodeList, int iteration) {
        double Fx = 0, Fy = 0;
        double fx = 0, fy = 0, gx = 0, gy = 0, hx = 0, hy = 0;
        boolean stop = false;

        Iterator nodeIt, tmpNodeIt, edgesFrom, edgesTo;

        Node currNode = null, edgeNode = null;
        Edge edge = null;
        Point p = null, q = null;

        double dist = 0;
        //Get all graph nodes and compute their forces
        nodeIt = nodeList.values().iterator();
        while (nodeIt.hasNext()) {
            currNode = (Node) nodeIt.next();

            if (currNode.nailed || !currNode.visible) {
                continue;
            }

            //**************************************************************************************
            //Calculate F forces (between node and its connected nodes)
            if (topKmode || starGraphMode) {
                edgesFrom = currNode.getSpeedEdgesFrom().iterator();
                edgesTo = currNode.getSpeedEdgesTo().iterator();
            } else {
                edgesFrom = currNode.getEdgesFrom().iterator();
                edgesTo = currNode.getEdgesTo().iterator();
            }
            while (edgesFrom.hasNext()) {
                edgeNode = ((Edge) edgesFrom.next()).getTargetNode();
                if (!edgeNode.visible || edgeNode == currNode) {
                    continue;
                }

                p = new Point(currNode.x, currNode.y);
                q = new Point(edgeNode.x, edgeNode.y);
                dist = calculateDistance(p, q);

                fx += Ks * (dist - Ls) * ((q.x - p.x) / dist);
                fy += Ks * (dist - Ls) * ((q.y - p.y) / dist);
            }//end while edgesFrom.hasNext
            while (edgesTo.hasNext()) {
                edgeNode = ((Edge) edgesTo.next()).getSourceNode();
                if (!edgeNode.visible || edgeNode == currNode) {
                    continue;
                }

                p = new Point(currNode.x, currNode.y);
                q = new Point(edgeNode.x, edgeNode.y);
                dist = calculateDistance(p, q);

                fx += Ks * (dist - Ls) * ((q.x - p.x) / dist);
                fy += Ks * (dist - Ls) * ((q.y - p.y) / dist);
            }//end while edgesTo.hasNext

            //**************************************************************************************
            //Calculate G forces (between all nodes in graph)
            tmpNodeIt = nodeList.values().iterator();
            while (tmpNodeIt.hasNext()) {
                edgeNode = (Node) tmpNodeIt.next();
                if (!edgeNode.visible || edgeNode == currNode) {
                    continue;
                }

                if (edgeNode != currNode) {
                    p = new Point(currNode.x + NodesWidth / 2, currNode.y + NodesHeigth / 2);
                    q = new Point(edgeNode.x + NodesWidth / 2, edgeNode.y + NodesHeigth / 2);
                    dist = calculateDistance(p, q);

                    gx += (Ke / Math.pow(dist, 2)) * ((p.x - q.x) / dist);
                    gy += (Ke / Math.pow(dist, 2)) * ((p.y - q.y) / dist);
                }//end if
            }//end tmpNodeIt.hasNext

            //**************************************************************************************
            //Calculate H forces (for all classes belong to connI)
            if (topKmode || starGraphMode) {
                edgesFrom = currNode.getSpeedEdgesFrom().iterator();
                edgesTo = currNode.getSpeedEdgesTo().iterator();
            } else {
                edgesFrom = currNode.getEdgesFrom().iterator();
                edgesTo = currNode.getEdgesTo().iterator();
            }
            while (edgesFrom.hasNext()) {
                edge = (Edge) edgesFrom.next();
                if (edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF
                        || edge.rdfObjectType == SEMWEB_OBJECT_TYPE.INSTANCEOF) {
                    edgeNode = edge.getTargetNode();
                    if (!edgeNode.visible || edgeNode == currNode) {
                        continue;
                    }

                    p = new Point(currNode.x, currNode.y);
                    q = new Point(edgeNode.x, edgeNode.y);

                    hx += Km * ((q.x - p.x) / Ls);
                    hy += Km * ((Ls + q.y - p.y) / Ls);
                }//end if
            }//end while edgesFrom.hasNext
            while (edgesTo.hasNext()) {
                edge = (Edge) edgesTo.next();
                if (edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF
                        || edge.rdfObjectType == SEMWEB_OBJECT_TYPE.INSTANCEOF) {
                    edgeNode = edge.getSourceNode();
                    if (!edgeNode.visible || edgeNode == currNode) {
                        continue;
                    }

                    p = new Point(currNode.x, currNode.y);
                    q = new Point(edgeNode.x, edgeNode.y);

                    hx += Km * ((q.x - p.x) / Ls);
                    hy -= Km * ((Ls + p.y - q.y) / Ls);
                }//end if
            }//end while edgesTo.hasNext

            //**************************************************************************************
            //Update node's forces data
            Fx = Math.round(fx + gx + hx);
            Fy = Math.round(fy + gy + hy);
            currNode.forceX = Fx;
            currNode.forceY = Fy;
            //**************************************************************************************
            //Check whether iteration should stop because to small force
            if (iteration % 10 == 0 && stop && Math.abs(Fx) <= 10 && Math.abs(Fy) <= 10) {
                stop = true;
            } else {
                stop = false;
            }

            //**************************************************************************************
            //Reset to zero all numbers
            fx = 0;
            fy = 0;
            gx = 0;
            gy = 0;
            hx = 0;
            hy = 0;
            Fx = 0;
            Fy = 0;
        }//end while nodeIt.hasNext
        return stop;
    }//end computeForces

    /**
     * Move all nodes respectively to their applied force
     * @param nodeList list with all graph nodes
     */
    public void moveNodes(Hashtable<String, Node> nodeList) {
        Iterator nodeIt = null;
        Node currNode = null;
        int moveX = 0, moveY = 0;
        int newX = 0, newY = 0;
        int Xplus = 0, Yplus = 0;
        int con = 1;
        int borderXrange = maxXrange / 20;
        int borderYrange = maxYrange / 20;

        //**************************************************************************************
        //Create a random number generator in order to get random coordinates
        //and avoid positioning nodes in drawing area limits if force is big enough
        //to push them over
        Date d = new Date();
        Random rand = new Random(d.getTime());

        //**************************************************************************************
        //Move each node according to the force applied to it
        nodeIt = nodeList.values().iterator();

        while (nodeIt.hasNext()) {
            currNode = (Node) nodeIt.next();
            //if node is nailed or invisible do not move it and continue with the next
            if (currNode.nailed || !currNode.visible) {
                continue;
            }

            //**************************************************************************************
            //if forceX is greater than Fthreshold then assign to it the max possible signed movement
            //move to X axis

            if (Math.abs(currNode.forceX) <= Fthres);//noop
            else {

                if ((currNode.forceX * con) <= Integer.MIN_VALUE) {
                    moveX = Integer.MIN_VALUE + 1;
                } else {
                    moveX = (int) (currNode.forceX * con);

                }

                if (Math.abs(moveX) > maxXmove) {
                    moveX = (moveX > 0 ? maxXmove : maxXmove * (-1));
                }

            }//end else

            //**************************************************************************************
            //if forceY is greater than Fthreshold then assign to it the max possible signed movement
            //move to Y axis

            if (Math.abs(currNode.forceY) <= Fthres);//noop
            else {
                if ((currNode.forceY * con) <= Integer.MIN_VALUE) {
                    moveY = Integer.MIN_VALUE + 1;
                } else {
                    moveY = (int) (currNode.forceY * con);

                }
                if (Math.abs(moveY) > maxYmove) {
                    moveY = (moveY > 0 ? maxYmove : maxYmove * (-1));
                }
            }//end else
            //**************************************************************************************



            newX = currNode.x + moveX;
            newY = currNode.y + moveY;
            Xplus = rand.nextInt(borderXrange);
            Yplus = rand.nextInt(borderYrange);


            //**************************************************************************************
            //Keep all nodes to the first quadrant(positive X, positive Y) and if force is big enough
            //to move one node more than the allowed movement range, move it as far as permitted

            if (newX < 0) {

                currNode.setCoreX(Xplus);
                if (newY < 0) {
                    currNode.setCoreY(Yplus);
                } else if (newY > maxYrange) {
                    currNode.setCoreY(maxYrange - Yplus);
                } else {
                    currNode.setCoreY(newY);
                }
            } else if (newX > maxXrange) {

                currNode.setCoreX(maxXrange - Xplus);
                if (newY < 0) {
                    currNode.setCoreY(Yplus);
                } else if (newY > maxYrange) {
                    currNode.setCoreY(maxYrange - Yplus);
                } else {
                    currNode.setCoreY(newY);
                }
            } else if (newY < 0) {

                currNode.setCoreY(Yplus);
                currNode.setCoreX(newX);
            } else if (newY > maxYrange) {

                currNode.setCoreY(maxYrange - Yplus);
                currNode.setCoreX(newX);
            } else {


                currNode.setCoreX(newX);
                currNode.setCoreY(newY);
            }
            //**************************************************************************************
            //Reset to Zero all numbers
            moveX = 0;
            moveY = 0;
            newX = 0;
            newY = 0;
        }//end while for move
    }//end moveNodes

    /**
     * Place nodes using Force-Directed Algorithm
     * @param params algorithm parameters.Null to use default values
     * @param realMove if false only the force computations are applied and 
     * not the real move
     * @return true if algorithm completed successfully, false otherwise
     */
    public boolean forceDirectedPlacement(String params, boolean realMove) {
        int iteration;
        int userChoice = 0;
        //Ask for user choice whether to continue every 10% of maximum iterations
        boolean stop = false;

        long startTime, stopTime;

        Hashtable<String, Node> smartNodeList = null;

        //Parse Parameters string and check if parameters are inserted correctly if param is not null.If it is null use default
        if (params != null) {
            if (!parseForceParameters(params)) {
                System.out.println("FAILED FDPA EXECUTION");
                return false;
                //**************************************************************************************
                //In order to perform faster drawing for large diagrams select the appropriate node list

            }
        }
        System.out.println("FDPA execution");
        if (topKmode) {
            if (topKnodeList == null || topKnodeList.isEmpty()) {
                System.out.println("In force directed application:topKnodeList EMPTY");
                return false;
            }
            smartNodeList = new Hashtable<String, Node>(topKnodeList);
        } else if (starGraphMode) {
            if (starGnodeList == null || starGnodeList.isEmpty()) {
                System.out.println("In force directed application:starGnodeList EMPTY");
                return false;
            }
            smartNodeList = new Hashtable<String, Node>(starGnodeList);
        } else {
            if (nodeList == null || nodeList.isEmpty()) {
                System.out.println("In force directed application:nodeList EMPTY");
                return false;
            }
            smartNodeList = new Hashtable<String, Node>(nodeList);
            //**************************************************************************************
        }

        //**************************************************************************************

        //Keep number of visible nodes
        int visibleNodes = 0;
        visGraph.updateGraph();
        //Make operations that enhance speed and update some values
        Iterator it = smartNodeList.values().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.nailed || !n.visible) {
                it.remove();
                System.out.println("FDPA:REMOVED NODE FOR PERFORMANCE:" + n.getName());
            } else {
                visibleNodes++;
                n.setStartingCoordinates(n.x, n.y);
            }
        }


        for (iteration = 0; iteration < MaxIterations; iteration++) {

            stop = computeForces(smartNodeList, iteration);

            moveNodes(smartNodeList);

            //Ask user choice whether to continue or not
            if (CheckStop != 0 && iteration % CheckStop == 0) {
                visGraph.updateGraph();
                userChoice = JOptionPane.showConfirmDialog(null, "Do you want to continue ?", "Continue", JOptionPane.YES_NO_OPTION);
                makeNodesInvisible();
            }
            if (stop || userChoice != 0) {
                System.out.println("Graph did not change during this loop. Iterations executed are : " + iteration);
                break;
            }
            stop = true;
        }//end for iteration < MaxIterations

        Iterator it2 = smartNodeList.values().iterator();
        while (it2.hasNext()) {
            Node n = (Node) it2.next();
            n.setEndingCoordinates(n.x, n.y);
        }

        if (realMove) {
            makeNodesInvisible();
            if (smoothLayoutTransition) {
                int prevScaleFactor = getScaleFactor();
                int newScaleFactor = scaleFactorBasedOnVisibleNodes(visibleNodes);
                scale(Math.min(prevScaleFactor, newScaleFactor));
                smoothLayoutTransition(smartNodeList, 20, visGraph);
                scale(prevScaleFactor);
            } else {
                applyNodesFinalPositions(smartNodeList);
                visGraph.updateGraph();
            }
        }

        
        return true;
    }

    private int scaleFactorBasedOnVisibleNodes(int visibleNodes) {
        int scale;

        if (visibleNodes <= 15) {
            scale = 100;
        } else if (visibleNodes <= 30) {
            scale = 90;
        } else if (visibleNodes <= 50) {
            scale = 80;

        } else if (visibleNodes <= 70) {
            scale = 70;
        } else if (visibleNodes <= 90) {
            scale = 60;
        } else {
            scale = 50;
        }

        return scale;
    }

    /**
     * Sets the status of smooth transitions
     * 
     * @param smoothTransitions - true for enabling smooth transitions and false otherwise
     */
    public void setSmoothTransitions(boolean smoothTransitions) {
        this.smoothLayoutTransition = smoothTransitions;
    }

    public boolean getSmoothTransitionsStatus() {
        return this.smoothLayoutTransition;
    }

    /**
     * Updates nodes position with smooth transitions
     *  
     * @param nodeList - the list of nodes that changed positions
     * @param smoothSteps - the number of steps in transition
     * @param affectedGraph - the graph that the transition is applied on
     */
    private void smoothLayoutTransition(Hashtable<String, Node> nodeList, int smoothSteps, VisualGraph affectedGraph) {
        //Steps for the user to see the difference


        for (int i = 0; i < smoothSteps; i++) {

            //Change the position of all nodes
            Iterator nodeIt = nodeList.values().iterator();
            while (nodeIt.hasNext()) {
                Node n = (Node) nodeIt.next();
                n.getGraphNode().setXYposition(n.x + (n.startPosX - n.endPosX) / (i + 1), n.y + (n.startPosY - n.endPosY) / (i + 1));
            }



//            Update the graphics directly
            affectedGraph.drawImage(affectedGraph.getGraphImage().getGraphics());

            //Apply a delay so the user can see the transition
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                //The exception should never occur
                ie.printStackTrace();
            }

        }
    }

    private void applyNodesFinalPositions(Hashtable<String, Node> nodeList) {
        Iterator nodeListIt = nodeList.values().iterator();
        while (nodeListIt.hasNext()) {
            Node n = (Node) nodeListIt.next();
            n.patchVirtualPosition();
        }
    }

    /**
     * New placement method. Place all equally ranked nodes in horizontal groups
     * @param params
     */
    public void horizontalPlacement(String params) {
        int xSpacing = 65;
        int ySpacing = 25;

        int x = 0;
        int y = 0;
        int maxPerLine = 0;

        Node node = null;
        double prevRank = -1;

        System.out.println("\n\n========================================");
        System.out.println("TopKnodes= " + topKnodes.length);
        for (int i = 0; i < topKnodes.length; i++) {
            node = (Node) topKnodes[i];
            System.out.println(node.getName() + " = " + node.rankingScore);

            if (node.rankingScore == prevRank) {
                if (maxPerLine > 30) {
                    ++y;
                    x = 0;
                    maxPerLine = 0;
                }
                node.setX(x * xSpacing);
                node.setY(y * ySpacing);
                ++x;
                ++maxPerLine;
            } else if (node.rankingScore > prevRank) {
                maxPerLine = 0;
                y += 2;
                x = 0;
                prevRank = node.rankingScore;
                node.setX(x * xSpacing);
                node.setY(y * ySpacing);
            }//end else
        }//end while nodeIt.hasNext
        visGraph.updateGraph();
    }//end horizontalPlacement

    /**
     * Add all Graph Elements (nodes and edges) to the visual graph so as to be visualized
     */
    public void visualizeGraphElements() {
        Iterator nodesIt = nodeList.values().iterator();
        Iterator edgesIt = edgeList.values().iterator();

        while (nodesIt.hasNext()) {
            Node n = (Node) nodesIt.next();
            visGraph.addNode(n);

        }

        while (edgesIt.hasNext()) {
            visGraph.addEdge((Edge) edgesIt.next());
        }
    }

    /**
     * Update graph view using layout to call the appropriate layout algorithm
     * @param layout identifier of layout algorithm
     * @param params parameters for the layout algorithm (responsible for their parsing)
     */
    public boolean updateLayout(String layout, String params, Point centerOfWindow) {
        boolean status = false;
        updateNodes();

        //Use the appropriate layout function according to layout string identifier
        if (layout.equals("random")) {
            status = randomLayout();
        } else if (layout.equals("topKnodes")) {
            if (topKNodes(params, centerOfWindow) == null) {
                status = false;
            }
            status = true;
        } else if (layout.equals("topKgroups")) {
            if (topKGroups(params) == null) {
                status = false;
            }
            status = true;
        } else if (layout.equals("forceDirected")) {

            status = forceDirectedPlacement(params, true);
        }



        visGraph.visualizeGraph();

        updateVisibility();       

        return status;
    }//end updateLayout

    /**
     * Return the visual representation of this graph
     * @return visual graph
     */
    public VisualGraph getVisualGraph() {
        return visGraph;
    }//end getVisualGraph

    /**
     * Scale visual graph
     * @param factor scale factor
     */
    public void scale(int factor) {
        visGraph.setScale((double) factor / 100);
        setScaleFactor(factor);
    }//end scale

    public void scaleCoordinates(int factor) {
        Iterator it = nodeList.values().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.visible) {
                n.setX((int) (n.x * ((double) factor / 100)));
                n.setY((int) (n.y * ((double) factor / 100)));
            }
        }
        setScaleCoordinateFactor(factor);
    }

    /**
     * Set input factor as scale factor for this graph
     * @param factor
     */
    public void setScaleFactor(int factor) {
        scaleFactor = factor;
    }//end setScaleFactor

    /**
     * Set input factor as a coordinate scale factor for this graph
     * @param factor - the factor for the scaling of coordinates only
     */
    public void setScaleCoordinateFactor(int factor) {
        scaleCoordinateFactor = factor;
    }

    /**
     * Return graph's current scale factor
     * @return factor
     */
    public int getScaleFactor() {
        return scaleFactor;
    }//end getScaleFactor

    /**
     * Return graph's current coordinate scale factor
     * @return scaleCoordinateFactor - the factor for the scaling of the coordinates
     */
    public int getScaleCoordinateFactor() {
        return scaleCoordinateFactor;
    }

    /**
     * Return graph's current nodes color
     * @return the color of the nodes
     */
    public Color getNodesColor() {
        return NodesColor;
    }

    /**
     * Update x and y positions for all nodes (usually called before applying a layout algorithm in order to update nodes postions that have changed by user)
     */
    public void updateNodes() {
        Iterator it = nodeList.values().iterator();

        while (it.hasNext()) {
            Node n = (Node) it.next();

            n.setX(n.getGraphNode().getXposition());
            n.setY(n.getGraphNode().getYposition());
            n.setVisible(n.visible);

        }//end while

    }//end updateNodes

    /**
     * Update nodes visibilty.
     */
    private void updateVisibility() {
        Iterator it = nodeList.values().iterator();

        while (it.hasNext()) {
            Node n = (Node) it.next();
            n.setVisible(n.visible);
        }
    }

    /**
     * Export visual graph as PNG image
     * @param fileName file where this image should be stored
     * @return true upon successful save, false otherwise
     */
    public boolean saveGraphAsImage(String fileName) {
        try {
            BufferedImage image = visGraph.getGraphImage();
            if (image == null) {
                return false;
            }

            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
            ImageIO.write(image, "png", out);
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }//end saveGraphAsImage

    /**
     * Save current graph in chosen file compliant to RDF Save Schema
     * @param fileName name of file that will hold the saved data
     */
    public void saveGraph(String fileName) {
        String str = "";

        //Updates the position of the nodes in case that were moved manually from the user
        updateNodes();

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            out.write("<?xml version=\"1.0\"?>\n");
            out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                    + "xmlns:graph=\"www.csd.uoc.gr/~leonidis/graph\">\n");
            out.write("\t<graph:Graph>\n");

            Iterator it2 = nameSpaces.iterator();
            while (it2.hasNext()) {
                String s = (String) it2.next();
                str = "\t\t<graph:savedNameSpaces>\n"
                        + "\t\t\t<graph:nsName>" + s + "</graph:nsName>\n"
                        + "\t\t</graph:savedNameSpaces>\n";
                out.write(str);

            }
            Iterator it = nodeList.values().iterator();
            while (it.hasNext()) {
                Node n = (Node) it.next();
                str += n.toString() + " : " + n.x + " , " + n.y + "\n";
                str = "\t\t<graph:Node>\n"
                        + "\t\t\t<graph:name>" + n.toString() + "</graph:name>\n"
                        + "\t\t\t<graph:coordX>" + n.x + "</graph:coordX>\n"
                        + "\t\t\t<graph:coordY>" + n.y + "</graph:coordY>\n"
                        + "\t\t\t<graph:nailed>" + n.nailed + "</graph:nailed>\n"
                        + "\t\t\t<graph:visible>" + n.visible + "</graph:visible>\n"
                        + "\t\t\t<graph:Rcolor>" + n.getGraphNode().getNodeGColor().getRed() + "</graph:Rcolor>\n"
                        + "\t\t\t<graph:Gcolor>" + n.getGraphNode().getNodeGColor().getGreen() + "</graph:Gcolor>\n"
                        + "\t\t\t<graph:Bcolor>" + n.getGraphNode().getNodeGColor().getBlue() + "</graph:Bcolor>\n"
                        + "\t\t\t<graph:width>" + n.getGraphNode().getNodeWidth() + "</graph:width>\n"
                        + "\t\t\t<graph:heigth>" + n.getGraphNode().getNodeHeigth() + "</graph:heigth>\n"
                        + "\t\t</graph:Node>\n";
                out.write(str);
            }//end while hasNext
            out.write("\t</graph:Graph>\n");
            out.write("</rdf:RDF>");
            out.close();

        }//end try
        catch (IOException IOe) {
            System.out.println(IOe.toString());
        }
    }//public void saveGraph

    /**
     * Restore a graph with saved data from chosen file
     * @param fileName name of file that holds the saved data
     */
    public void restoreGraph(RDFModel model, String fileName) {
        try {
            File file = new File(fileName);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element " + doc.getDocumentElement().getNodeName());
            NodeList nodes = doc.getElementsByTagName("graph:Node");

            NodeList nameSpaceNodes = doc.getElementsByTagName("graph:savedNameSpaces");


            Node node = null;
            org.w3c.dom.Node savedNode = null;
            org.w3c.dom.Node nsSavedNode = null;

            for (int i = 0; i < nameSpaceNodes.getLength(); i++) {
                nsSavedNode = nameSpaceNodes.item(i);
                if (nsSavedNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    this.nameSpaces.add(((Element) nsSavedNode).getElementsByTagName("graph:nsName").item(0).getChildNodes().item(0).getNodeValue());
                }
            }

            populateGraph(model);
            createGraphRanker();
            visualizeGraphElements();

            for (int s = 0; s < nodes.getLength(); s++) {
                savedNode = nodes.item(s);
                if (savedNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    node = (Node) nodeList.get(((Element) savedNode).getElementsByTagName("graph:name").item(0).getChildNodes().item(0).getNodeValue());
                    if (node != null) {
                        node.setX(new Integer(((Element) savedNode).getElementsByTagName("graph:coordX").item(0).getChildNodes().item(0).getNodeValue()));
                        node.setY(new Integer(((Element) savedNode).getElementsByTagName("graph:coordY").item(0).getChildNodes().item(0).getNodeValue()));
                        //Restore the nodes color
                        Integer rcolor = (new Integer(((Element) savedNode).getElementsByTagName("graph:Rcolor").item(0).getChildNodes().item(0).getNodeValue()));
                        Integer gcolor = (new Integer(((Element) savedNode).getElementsByTagName("graph:Gcolor").item(0).getChildNodes().item(0).getNodeValue()));
                        Integer bcolor = (new Integer(((Element) savedNode).getElementsByTagName("graph:Bcolor").item(0).getChildNodes().item(0).getNodeValue()));
                        Color c = new Color(rcolor, gcolor, bcolor);
                        node.getGraphNode().setNodeGColor(c);
                        //Restore the size of the node
                        Double nWidth = (new Double(((Element) savedNode).getElementsByTagName("graph:width").item(0).getChildNodes().item(0).getNodeValue()));
                        Double nHeigth = (new Double(((Element) savedNode).getElementsByTagName("graph:heigth").item(0).getChildNodes().item(0).getNodeValue()));
                        node.getGraphNode().setNodebounds(nWidth, nHeigth);

                        if (((Element) savedNode).getElementsByTagName("graph:nailed").item(0).getChildNodes().item(0).getNodeValue().equals("true")) {
                            node.setNailed(true);
                        } else {
                            node.setNailed(false);
                        }

                        if (((Element) savedNode).getElementsByTagName("graph:visible").item(0).getChildNodes().item(0).getNodeValue().equals("true")) {
                            node.setVisible(true);
                        } else {
                            node.setVisible(false);
                        }


                    }//end if node
                }//end if savedNode

            }//end for nodes.getLength
            visGraph.visualizeGraph();
            makeNodesInvisible();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//end restoreGraph

    /**
     * Return if visual graph can perform an undo operation
     * @return true if it can, false otherwise
     */
    public boolean canUndo() {
        return visGraph.canUndo();
    }

    /**
     * Return if visual graph can perform an redo operation
     * @return true if it can, false otherwise
     */
    public boolean canRedo() {
        return visGraph.canRedo();
    }

    /**
     * Perform an undo operation if possible
     */
    public void undo() {
        if (visGraph.canUndo()) {
            visGraph.undo();
        }
    }

    /**
     * Perform a redo operation if possible
     */
    public void redo() {
        if (visGraph.canRedo()) {
            visGraph.redo();
        }
    }

    /**
     * A function that returns the graph in its original state.
     * All nodes visible,Topk and StarGraph browsing deactivated
     */
    public void graphView() {
        deacivateStarGraph();
        deactivateTopK();
        makeNodesVisible();
        makeNodesUnailed();
        randomLayout();
    }

    /**
     * Find all nodes with distance "radius" from the given collection of nodes
     * @param nodes - the initial set of nodes
     * @param radius - the distance between nodes
     */
    private void starView(Collection<Node> nodes, int radius) {
        ConcurrentLinkedQueue<Node> Q = new ConcurrentLinkedQueue<Node>(nodes);

        while (!Q.isEmpty() && radius != 0) {
            Node n = Q.remove();
            ConcurrentLinkedQueue<Node> Qnew = new ConcurrentLinkedQueue<Node>();
            //IF WE APPLY STARVIEW IN DATA WE ARE NOT SURE THAT DATA ARE LOADED
            if (n.isInstance() && !iCache.contains(n.getName())) {
                System.out.println("ExecuteQuery to retrieve " + n.getName());
                Client c = ProjectManager.getSingleton().getActiveProject().getActiveClient();
                addInstanceAndNeighbours(n.getName(), c);
                //Executer StarView from the begining after repopulation
                executeStarView();

            }
            for (Edge e : n.getEdgesFrom()) {
                Node node = e.getTargetNode();
                if (!node.isVisible()) {
                    node.visible = true;
                    e.setVisible(true);
                    Qnew.add(node);
                    //Add node to starGnodeList for speed performance in layout algorithms
                    starGnodeList.put(node.getName(), node);
                }
                if (this.viewIndirectEdges) {
                    e.setVisible(true);
                }
            }
            for (Edge e : n.getEdgesTo()) {
                Node node = e.getSourceNode();

                if (!node.isVisible()) {
                    e.setVisible(true);
                    node.visible = true;
                    Qnew.add(node);
                    //Add node to starGnodeList for speed performance in layout algorithms
                    starGnodeList.put(node.getName(), node);
                }
                if (this.viewIndirectEdges) {
                    e.setVisible(true);
                }
            }
            starView(Qnew, radius - 1);
        }
    }

    /**
     * The algorithm selects all the neighbour's of the selected node with the given radius
     * @param centerOfwindow - the x,y coordinates of the center of the window(The selected noded will be moved there)
     */
    public void executeStarView() {
        Point centerOfwindow = ProjectManager.getSingleton().getActiveProject().getActiveFrame().getCenterOfFrame();

        Node nodes[] = getSelectedNodes();
        if (nodes.length != 1) {
            //if selected nodes != 1 return and do nothing
            System.out.println("Starview to nodes ->" + nodes.length + " != 1:Starview cancelling...");
            return;
        }
        long s_start = System.currentTimeMillis();
        System.out.println("Starting StarView");
        //If the node is visible we don't want all connected edges to
        //be visible as well
        visGraph.getGraphLayoutCache().setShowsExistingConnections(false);
        //We make all edges invisible in order to present the ones that
        //we need depending on the svIndirectEdges value
        long overhStart = System.currentTimeMillis();
        makeEdgesInVisible();

        long overhEnd = System.currentTimeMillis();

        overhStart = System.currentTimeMillis();
        //Initialize all nodes to be invisible and positioned random in 50_50box

        randomPlacementIn50_50BOX(nodeList.values(), centerOfwindow);

        overhEnd = System.currentTimeMillis();

        overhStart = System.currentTimeMillis();

        //A changed version of the bfs algorithm
        ConcurrentLinkedQueue<Node> Q = new ConcurrentLinkedQueue<Node>();
        nodes[0].visible = true;
        nodes[0].setX(centerOfwindow.x);
        nodes[0].setY(centerOfwindow.y);
        nodes[0].nailed = true;
        Q.add(nodes[0]);


        //All nodes that appear in StarView are entered in another
        //nodeList to improve speed in layout algorithms
        starGnodeList.clear();
        starGnodeList.put(nodes[0].getName(), nodes[0]);
        starView(Q, this.starViewRadius);
        overhEnd = System.currentTimeMillis();
        overhStart = System.currentTimeMillis();
        completeEdgeSpeedList(starGnodeList);
        overhEnd = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        System.out.println("ENDING STARVIEW TIME =" + (end - s_start));
    }

    //Places a collection of nodes inside a 50_50 box defined frome the given
    //box center.It also makes the nodes invisible and unailed.
    //Used with starView
    private void randomPlacementIn50_50BOX(Collection<Node> nodes, Point boxCenter) {
        for (Node n : nodes) {
            n.visible = false;
            n.nailed = false;
            n.setCoreX(randomCoordinateIn50_50Box(boxCenter));
            n.setCoreY(randomCoordinateIn50_50Box(boxCenter));
        }
    }

    /**
     * Set the radius of the StarView
     * @param radius - an integer that shows the radius of the StarView mode
     */
    public void setStarViewRadius(int radius) {
        this.starViewRadius = radius;
    }

    /**
     * Sets the repulsion strength
     *
     * @param Ke - the new repulsion strength
     */
    public void setKe(double Ke) {
        //TODO MAX VALUE something must be set
        //            if(Ke > kati)
        //MIN VALUE for Ke
        if (Ke < Km) {
            this.Ke = Km;
        } else {

            this.Ke = Ke;
        }
    }

    /**
     * Sets the magnetic field strength
     *
     * @param Ke - the new magnetic field strength
     */
    public void setKm(double Km) {
        //MAX VALUE for Km
        if (Km > Ke) {
            this.Km = Ke;
        } else if (Km < 15) {
            this.Km = 15;
        } else {
            this.Km = Km;
        }

    }

    /**
     * Sets the max iteration for the fdpa
     *
     * @param maxIter - the new number of iterations
     */
    public void setMaxIterations(int maxIter) {
        this.MaxIterations = maxIter;
    }

    /**
     * Set the value for viewing the indirect edges in StarView
     * @param yes_or_no - true if we want the indirect edges to appear and false otherwise
     */
    public void setStarViewIndirectEdgesAppearing(boolean yes_or_no) {
        this.viewIndirectEdges = yes_or_no;
    }

    private int randomCoordinateIn50_50Box(Point boxCenter) {
        Random rand = new Random();
        int inWidth = boxCenter.x + 50;
        int inHeigth = boxCenter.y + 50;
        int maxval;
        int minval;
        if (inWidth > inHeigth) {
            maxval = inWidth;
        } else {
            maxval = inHeigth;
        }

        inWidth = boxCenter.x - 50;
        inHeigth = boxCenter.y - 50;

        if (inWidth < inHeigth) {
            minval = inWidth;
        } else {
            minval = inHeigth;
        }
        if (minval < 0) {
            minval = 0;
        }

        return (rand.nextInt(50) + minval) % maxval;
    }

    /*
     * Every node has some list with edges that are currently visible "speedEdges"
     * This function takes an iterator from a list of visible nodes and completes the lists.
     * The list's are used for performance in layout algorithms
     */
    private void completeEdgeSpeedList(Hashtable<String, Node> nodeList) {
        Iterator edgesFromIt = null, edgesToIt = null;
        Node n, tmpNode;
        Edge edge;
        Iterator forNodeList = nodeList.values().iterator();

        while (forNodeList.hasNext()) {
            n = (Node) forNodeList.next();
            n.getSpeedEdgesFrom().clear();
            n.getSpeedEdgesTo().clear();
            edgesFromIt = n.getEdgesFrom().iterator();
            edgesToIt = n.getEdgesTo().iterator();

            while (edgesFromIt.hasNext()) {
                edge = (Edge) edgesFromIt.next();
                tmpNode = edge.getTargetNode();
                if (tmpNode.visible) {
                    n.addSpeedEdgeFrom(edge);
                }
            }//end while edgesFromIt.hasNext

            while (edgesToIt.hasNext()) {
                edge = (Edge) edgesToIt.next();
                tmpNode = edge.getSourceNode();
                if (tmpNode.visible) {
                    n.addSpeedEdgeTo(edge);
                }
            }//end while edgesToIt.hasNext
        }
    }

    /**
     * Make visible all nodes whose visibility status is true
     */
    public void makeNodesVisible() {
        Iterator it = nodeList.values().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            //Instance Nodes are not revealed only the schema
            if (!n.isInstance()) {
                n.setVisible(true);
            } else {
                n.setVisible(false);
            }
            deactivateTopK();
            deacivateStarGraph();
            topKnodeList.clear();
        }//end while
        visGraph.updateGraph();

    }//end makeNodesVisible

    public void makeNodesUnailed() {
        Iterator it = nodeList.values().iterator();
        while (it.hasNext()) {
            ((Node) it.next()).setNailed(false);
        }
        deactivateTopK();
    }

    /**
     * Make invisible all nodes whose visibility status is false
     */
    public void makeNodesInvisible() {
        Iterator it = nodeList.values().iterator();
        Node n = null;

        while (it.hasNext()) {
            n = ((Node) it.next());
            if (!n.visible) {
                n.setVisible(false);
            }
        }//end while
    }//end makeNodesVisible

    /**
     * Make all nodes in the parameter ivisible
     *
     * @param nodes
     */
    private void makeNodesInVisible(Collection<Node> nodes) {
        for (Node n : nodes) {
            n.setVisible(false);
        }
    }

    /**
     * Makes all edges of the graph invisible
     */
    private void makeEdgesInVisible() {
        for (Edge e : edgeList.values()) {
            if (e.visible) {
                e.setVisible(false);
            }
        }
    }

    /**
     * Makes the edges given as parameter invisible
     */
    private void makeEdgesInVisible(Collection<Edge> edges) {
        for (Edge e : edges) {
            if (e.visible) {
                e.setVisible(false);
            }
        }
    }

    /**
     * Makes all edges of the graph visible
     */
    private void makeEdgesVisible() {
        for (Edge e : edgeList.values()) {
            if (!e.visible) {
                e.setVisible(true);
            }
        }
    }

    /**
     * Sets the visibility status of all edge-labels in the graph
     *
     * @param visibility - true for visible labels and false for invisible
     */
    public void setEdgeLabelsVisibility(boolean visibility) {
        this.edgesLabelsVisibility = visibility;
        for (Edge e : edgeList.values()) {
            e.setLabelVisible(visibility);
        }
    }

    /**
     * Returns the visibily status of the edge-labels
     * @return true if edge-labels are visible and false otherwise
     */
    public boolean getEdgesLabelsVisibility() {
        return this.edgesLabelsVisibility;
    }

    /**
     * Hide all selected nodes
     */
    public void hideSelectedNodes() {

        Node n[] = getSelectedNodes();

        for (int i = 0; i < n.length; i++) {
            nodeList.get(n[i].getName()).setVisible(false);
        }
        visGraph.updateGraph();

    }

    public void nailSelectedNodes(boolean true_false) {
        Node n[] = getSelectedNodes();

        for (int i = 0; i < n.length; i++) {
            n[i].setNailed(true_false);
        }
    }

    public void setDependencyLoad(Project.DEPENDENCIES dType) {
        this.dependencyType = dType;
    }

    public void setGraphNameSpaces(String[] nspaces) {
        this.nameSpaces.clear();
        if (nspaces == null) {
            return;
        }
        for (String namespace : nspaces) {
            if (namespace == null || namespace.trim().isEmpty()) {
                continue;
            }
            if (!this.nameSpaces.contains(namespace)) {
                this.nameSpaces.add(namespace);
            }
        }

    }

    /**
     * Sets the default gradient color of the nodes of the graph that belong to the "selNameSpace" namespace
     * @param GColor the gradient color
     * @param selNameSpace the nameSpace that we will change the color
     */
    public void setGraphGradientColor(Color GColor, String selNameSpace) {
        NodesColor = GColor;
        Iterator it = nodeList.values().iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.getNodeNamespace().equals(selNameSpace)) {
                n.getGraphNode().setNodeGColor(GColor);
            }

        }

        visGraph.updateGraph();
    }

    /**
     * Sets the default size for the nodes of the graph
     *
     * @param w the width of the node
     * @param h the heigth of the node
     */
    public void setNodesDefaultSize(int w, int h) {
        if (w > 0 && h > 0) {
            NodesWidth = w;
            NodesHeigth = h;
            Iterator it = nodeList.values().iterator();
            while (it.hasNext()) {
                ((Node) it.next()).getGraphNode().setNodebounds(w, h);
            }
            visGraph.updateGraph();
        }
    }

    /**
     * Puts the namespaces of the selected nodes in a String array and returns them
     * @return a String[] with the namespaces inside
     */
    public String[] getSelectedNodeNamespaces() {
        Node[] nodes = getSelectedNodes();
        ArrayList<String> tempNamespaces = new ArrayList<String>();
        for (Node n : nodes) {
            if (!tempNamespaces.contains(n.getNodeNamespace())) {
                tempNamespaces.add(n.getNodeNamespace());
            }
        }
        String[] result = new String[tempNamespaces.size()];
        int i = 0;
        Iterator it = tempNamespaces.iterator();
        while (it.hasNext()) {
            result[i] = (String) it.next();

        }
        return result;
    }

    /**
     * String representation = "forceX forceY startX startY endX endY"
     *
     * @return a string representation with the forces from the selected node
     */
    public String getSelectedNodeForces() {
        Node[] nodes = getSelectedNodes();
        return nodes[0].forceX + " " + nodes[0].forceY + " " + nodes[0].startPosX + " " + nodes[0].startPosY + " " + nodes[0].endPosX + " " + nodes[0].endPosY + " ";
    }

    /**
     * Create an array that contains all nodes that user selected in visual graph
     * @return an array with selected nodes
     */
    public Node[] getSelectedNodes() {
        int selectedNo = visGraph.getSelectedNodesCount();
        Node[] selected = new Node[selectedNo];

        for (int i = 0; i < selectedNo; i++) {
            selected[i] = ((GraphNode) visGraph.getSelectionCells()[i]).getParentNode();
        }
        return selected;
    }

    /**
     * Based on the the given name select the node if exists
     * @param nodeName - the name of  the node to be selected
     */
    public void setSelectedNode(String nodeName) {
        Node n = nodeList.get(nodeName);
        if (n == null) {
            System.out.println("Node does not exist");
        } else {
            System.out.println(n.getName());
            //if we select a node it is obviously visible
            n.setVisible(true);

        }
        visGraph.setSelectionCell(n.getGraphNode());

    }

    /**
     * Select all nodes in visual graph
     */
    public void getAllNodes() {
        Iterator it = nodeList.values().iterator();
        int i = 0;
        Node n;
        Object[] allNodes = new Object[nodeList.size()];
        while (it.hasNext()) {
            n = ((Node) it.next());
            allNodes[i++] = n.getGraphNode();
        }
        visGraph.setSelectionCells(allNodes);
    }//end getAllNodes

    /**
     * Select in visual graph all nodes connected with the current selected node
     */
    public void getAllConnectedNodes() {

        try {
            Node startNode = ((GraphNode) visGraph.getSelectionCells()[0]).getParentNode();
            Object[] connectedNodes = new Object[startNode.getEdgesFrom().size() + startNode.getEdgesTo().size()];
            int i = 0;
            Node n;
            Iterator edgeFrom = startNode.getEdgesFrom().iterator();
            Iterator edgeTo = startNode.getEdgesTo().iterator();

            while (edgeFrom.hasNext()) {
                n = ((Edge) edgeFrom.next()).getTargetNode();
                connectedNodes[i++] = n.getGraphNode();
            }
            while (edgeTo.hasNext()) {
                n = ((Edge) edgeTo.next()).getSourceNode();
                connectedNodes[i++] = n.getGraphNode();
            }
            visGraph.setSelectionCells(connectedNodes);
        }//end try
        catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }//end getAllConnectedNodes

    /**
     * Select in visual graph all subclass nodes connected with the current selected node
     */
    public void getAllSubclassNodes() {
        try {
            Node startNode = ((GraphNode) visGraph.getSelectionCells()[0]).getParentNode();
            Iterator edgesTo = startNode.getEdgesTo().iterator();
            Edge edge;
            ArrayList<GraphNode> list = new ArrayList<GraphNode>();
            Object[] subclassNodes;

            while (edgesTo.hasNext()) {
                edge = (Edge) edgesTo.next();
                if (edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF) {
                    list.add(edge.getSourceNode().getGraphNode());
                }
            }
            subclassNodes = list.toArray();
            visGraph.setSelectionCells(subclassNodes);
        }//end try
        catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }//end getAllSubclassNodes

    /**
     * Select in visual graph all nodes of the specified namespace
     *
     * @param namespace-the namespace that we want to select the nodes
     */
    public void getAllNodesOfNamespace(String namespace) {
        Iterator it = nodeList.values().iterator();
        ArrayList<GraphNode> list = new ArrayList<GraphNode>();
        Object[] namespaceNodes;

        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.getNodeNamespace().equals(namespace)) {
                list.add(n.getGraphNode());
            }
        }
        namespaceNodes = list.toArray();
        visGraph.setSelectionCells(namespaceNodes);
    }

    /**
     * Get Ks parameter used in Force-Directed Placement Algorithm
     * @return Ks
     */
    public double getKs() {
        return Ks;
    }

    /**
     * Get Km parameter used in Force-Directed Placement Algorithm
     * @return Km
     */
    public double getKm() {
        return Km;
    }

    /**
     * Get Ls parameter used in Force-Directed Placement Algorithm
     * @return Ls
     */
    public double getLs() {
        return Ls;
    }

    /**
     * Get Ke parameter used in Force-Directed Placement Algorithm
     * @return Ke
     */
    public double getKe() {
        return Ke;
    }

    /**
     * Get MaxIterations parameter used in Force-Directed Placement Algorithm
     * @return MaxIterations
     */
    public int getMaxIterations() {
        return MaxIterations;
    }

    /**
     * Get CheckStop parameter used in Force-Directed Placement Algorithm
     * @return CheckStop
     */
    public int getCheckStop() {
        return CheckStop;
    }

    /**
     * Get maxXmove parameter used in Force-Directed Placement Algorithm
     * @return maxXmove
     */
    public int getmaxXmove() {
        return maxXmove;
    }

    /**
     * Get maxYmove parameter used in Force-Directed Placement Algorithm
     * @return maxYmove
     */
    public int getmaxYmove() {
        return maxYmove;
    }

    /**
     * Get Fthres parameter used in Force-Directed Placement Algorithm
     * @return Fthres
     */
    public int getFthres() {
        return Fthres;
    }

    /**
     * Get the default width of the graph nodes
     *
     * @return the width of the nodes of the graph
     */
    public int getGraphNodesWidth() {
        return NodesWidth;
    }

    /**
     * Get the default heigth of the graph nodes
     *
     * @return the heigth of the nodes of the graph
     */
    public int getGraphNodesHeigth() {
        return NodesHeigth;
    }

    public Collection<String> getNamespaces(){
        return nameSpaces;
    }

    /**
     *
     * @return the current radius for star view
     */
    public int getStarViewRadius() {
        return starViewRadius;
    }

    /**
     * Returns the cache that holds the instances for the graph
     *
     * @return the cache that holds the instances
     */
    public InstanceCache getInstanceCache() {
        return iCache;
    }

    public Hashtable<String, Point> find(String findName, String class_or_property) {
        Hashtable<String, Point> results = new Hashtable<String, Point>();


        if (class_or_property.equals("classFind")) {
            Iterator it = nodeList.values().iterator();
            while (it.hasNext()) {
                Node n = (Node) it.next();
                if (n.getName().contains(findName) && n.visible) {
                    Point p = new Point();
                    p.x = n.getGraphNode().getXposition();
                    p.y = n.getGraphNode().getYposition();
                    visGraph.setSelectionCell(n);
                    results.put(n.getName(), p);

                }
            }

        } else {//Find Property
            Iterator it = edgeList.values().iterator();
            while (it.hasNext()) {
                Edge e = (Edge) it.next();

                if (e.toString().contains(findName)) {
                    Point p = new Point();
                    p.x = (e.getSourceNode().x + e.getTargetNode().x) / 2;
                    p.y = (e.getSourceNode().y + e.getTargetNode().y) / 2;
                    results.put(e.toString(), p);
                }
            }
        }
        return results;
    }

    /**
     * Checks the existance of the selected nodes namespaces in the graph
     *
     * @return true if the node's namespace is included in the graph and false otherwise
     */
    public boolean nodeNSexistINGraph(String[] nspaces) {

        if (nspaces.length != 1) {
            return false;
        }

        for (String ns : nspaces) {
            boolean exists = false;
            for (String graphNspace : this.nameSpaces) {
                if (graphNspace.equals(ns)) {
                    exists = true;
                }

            }
            if (!exists) {

                return false;
            }
        }
        return true;
    }

    private Color generateNameSpaceColor() {
        Color c;
        Random f = new Random();
        c = new Color(f.nextInt(255), f.nextInt(255), f.nextInt(255));
        return c;
    }

    /**
     * Activates star-browsing in this graph
     */
    public void activateStarGraph() {
        deactivateTopK();
        this.starGraphMode = true;
        starGnodeList.clear();
    }

    /**
     * Deactivates star-browsing in this graph
     */
    public void deacivateStarGraph() {
        this.starGraphMode = false;
        makeEdgesVisible();
        visGraph.getGraphLayoutCache().setShowsExistingConnections(true);
    }

    /**
     * Activates the top-k mode along with other actions that must be made
     */
    public void activateTopK() {
        deacivateStarGraph();
        this.topKmode = true;
    }

    /**
     * Deactivates the top-k mode along with other actions that must be made
     */
    public void deactivateTopK() {
        this.topKmode = false;
        topKnodeList.clear();
    }

    /**
     * Checks if starGrapg browsing is enabled or not
     * @return true if starGraph browsing is enabled and false otherwise
     */
    public boolean isStarGraphActivated() {
        return starGraphMode;
    }


    public void applyMagicCorrection(String type) {
        CorrectingActions cA = new CorrectingActions(Km, Ke, getLayoutMetrics2());
        if (type.equals("Both")) {

            cA.improveBoth15();//DATE 26-3-2010

        } else if (type.equals("V")) {
            cA.improveVerticality();
        } else if (type.equals("A")) {
            cA.improveArea();
        }
        double iKm = cA.getImprovedKm();
        double iKe = cA.getImprovedKe();
        setKe(iKe);
        setKm(iKm);
        updateLayout("forceDirected", null, null);
    }


    public LayoutMetrics2 getLayoutMetrics2() {
        return new LayoutMetrics2(nodeList.values(), edgeList.values());
    }
}//end class Graph

/**
 * Custom comparator for node objects which uses node's ranking score member
 * @author leonidis
 *
 */
class myComparator implements Comparator<Object> {

    /**
     * Method that compares two objects by comparing their ranking score values
     */
    public int compare(Object o1, Object o2) {
        if (((Node) o1).rankingScore < ((Node) o2).rankingScore) {
            return -1;
        } else if (((Node) o1).rankingScore > ((Node) o2).rankingScore) {
            return 1;
        } else {
            return 0;
        }
    }
}//end class myComparator
