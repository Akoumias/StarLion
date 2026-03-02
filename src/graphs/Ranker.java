/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package graphs;

import model.GraphModel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * A class that provides a ranking of nodes according to the selected ranking
 * method
 * @author zabetak
 */
public class Ranker {
    //Namespaces to be taken into account in rank methods
	private String 					XMLNamespace = "http://www.w3.org/2001/XMLSchema#";
	private String 					RDFNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private String 					RDFSNamespace = "http://www.w3.org/2000/01/rdf-schema#";


    private Hashtable<String,Node> nodeList;
    private ArrayList<Edge> edgeList;
    private Object[]          rankedNodes;
    //Variables used in method5
	private double 					q1 = 0.30;
	private double 					q2 = 0.5;
	private double 					q3 = 0.2;
	//**************************************************************************************
    //Used to get bound for method2
	private int 					bound = 0;

    /**
     * Creates a new ranker for the given collection of nodes that are connected
     * with given edges
     * @param nodes - the collection of nodes to be ranked
     * @param edges - the collection of edges that represent the connection of the
     * nodes
     */
    public Ranker(Collection<Node> nodes,Collection<Edge> edges){
        Iterator collectionIt = nodes.iterator();
        nodeList = new Hashtable<String, Node>(nodes.size());

        while(collectionIt.hasNext()){
            Node n =(Node) collectionIt.next();
            nodeList.put(n.getName(),n);
        }

        collectionIt = edges.iterator();
        edgeList = new ArrayList<Edge>(edges.size());
        while(collectionIt.hasNext()){
            Edge e = (Edge) collectionIt.next();
            edgeList.add(e);
        }
    }

	/**
	 * Constructor that initializes the Ranker with a GraphModel
	 * @param graphModel the graph model to rank
	 * @author jakoum
	 */
	public Ranker(GraphModel graphModel) {
		// Get the underlying Graph object for now
		// In the future, we'll directly use the GraphModel's nodes and edges
		Graph graph = graphModel.getGraph();

		// Initialize with the nodes and edges from the graph
		Collection<Node> nodes = graph.getNodeList().values();
		Collection<Edge> edges = graph.getEdgeList().values();

		// Initialize the node list
		Iterator collectionIt = nodes.iterator();
		nodeList = new Hashtable<String, Node>(nodes.size());
		while(collectionIt.hasNext()){
			Node n = (Node) collectionIt.next();
			nodeList.put(n.getName(), n);
		}

		// Initialize the edge list
		collectionIt = edges.iterator();
		edgeList = new ArrayList<Edge>(edges.size());
		while(collectionIt.hasNext()){
			Edge e = (Edge) collectionIt.next();
			edgeList.add(e);
		}
	}

	/**
     * Get the nodes ranked.
     * @return
     */
    public Object[] getRankedNodes(){
        return rankedNodes;
    }

    /**
	 * Get Q1 parameter used in method 2
	 * @return q1
	 */
	public double getQ1(){
		return q1;
	}

	/**
	 * Get Q2 parameter used in method 2
	 * @return q2
	 */
	public double getQ2(){
		return q2;
	}

	/**
	 * Get Q3 parameter used in method 2
	 * @return q3
	 */
	public double getQ3(){
		return q3;
	}

    /**
	 * Graph Degree ranking method
	 * Calculate node's rank by adding Subclasses+Superclasses+Domains+Ranges
	 * @param namespace_uri chosen namespace
	 * @return a HashTable with ConnectivityInfo objects that holds all ranks
	 */
	public Hashtable<String, ConnectivityInfo>  method0( String namespace_uri){
		//Returning value
                //long start = System.currentTimeMillis();
                //System.out.println("Starting METHOD 0");
		Hashtable<String, ConnectivityInfo> nodeRanks = null;
		ConnectivityInfo conInfo = null;
		//Temporal Node and Edge objects
		Node node = null;
		Edge edge = null;
		String targetNodeNamespaceUri = "";
		//Iterators for each node's edge lists -incoming and outgoing-
		Iterator edgeFromIt = null, edgeToIt = null;
		//Variables regarding the number of properties for each node (both as domain and as range class)
		int domainsCl = 0, rangesCl = 0;
		//Variable that holds the rank for each node
		double rank = 0;

		//**************************************************************************************
		//Initialize Hashtable and Iterator for all contained nodes in this graph
		nodeRanks = new Hashtable<String, ConnectivityInfo>(nodeList.size());
		Iterator nodeIt = nodeList.values().iterator();

		/*
		 * For each node in this graph evaluate all the subelements needed to calculate its rank
		 * Check all its edges and increment the appropriate variables
		 * Be careful -> If the target node does not belong in one of the allowable namespaces
		 * cannot be count as a valid property for this ranking method
		 * Allowable namespaces :
		 * 1. The loaded-one
		 * 2. XML Schema
		 * 3. RDF
		 * 4. RDFS
		 */
		while(nodeIt.hasNext()){
			//For each node create a new connectivity element
			node = (Node)nodeIt.next();
			conInfo = new ConnectivityInfo(node.getName(),0);

			//**************************************************************************************
			//Check all outgoing edges
			edgeFromIt = node.getEdgesFrom().iterator();
			while(edgeFromIt.hasNext()){
				edge = (Edge)edgeFromIt.next();
				targetNodeNamespaceUri = edge.getTargetNode().getNodeUri();
				if(
					edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY &&
					(
					 targetNodeNamespaceUri.contains(namespace_uri) ||
					 targetNodeNamespaceUri.contains(XMLNamespace) ||
					 targetNodeNamespaceUri.contains(RDFNamespace) ||
					 targetNodeNamespaceUri.contains(RDFSNamespace)
					)
				)
						domainsCl++;
			}//end while edgesFrom

			//**************************************************************************************
			//Check all incoming edges
			edgeToIt = node.getEdgesTo().iterator();
			while(edgeToIt.hasNext()){
				edge = (Edge)edgeToIt.next();
				if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY)
					rangesCl++;
			}//end while edgesTo

			//**************************************************************************************
			//For each node set the compulsory fields for rank calculation
                        conInfo.setSubclassesNo((node.getSubClasses(true)).size());
                        conInfo.setSuperclassesNo((node.getSuperClasses(true)).size());
			conInfo.setRangeclassesNo(rangesCl);
			conInfo.setDomainclassesNo(domainsCl);

			//**************************************************************************************
			//Calculate rank : SuperClasses + SubClasses + AllProperties
			rank = conInfo.getSuperclassesNo() + conInfo.getSubclassesNo() + conInfo.getDomainclassesNo() + conInfo.getRangeclassesNo();
			//**************************************************************************************
			//Update node's and connectivity element's rank and add it into nodeRanks table
			node.rankingScore = rank;
			conInfo.setRank(rank);
			nodeRanks.put(node.getName(), conInfo);
			//**************************************************************************************
			//Restart counters
			domainsCl = 0;
			rangesCl = 0;
		}//end while nodeIt.hasNext
		generateSortedTopK();
                //long end = System.currentTimeMillis();
                //System.out.println("ENDING METHOD 0:"+(end-start));
		return nodeRanks;
	}//end degreeRank


	private int method2Phase1( ){
		int Attrs = 0;
		String namespaceUri = "";
		//Node and edge objects required for obtaining temporal data
		Node node = null;
		Edge edge = null;
		//Iterators to scan through lists of edges (from and to, both located in each node) and the list of all graph nodes
		Iterator edgeFromIter = null, nodeIt = null;

		//Calculate the graph's total number of attrs
		nodeIt = nodeList.values().iterator();
		while(nodeIt.hasNext()){
			node = (Node)nodeIt.next();
			edgeFromIter = node.getEdgesFrom().iterator();
			while(edgeFromIter.hasNext()){
				edge = (Edge)edgeFromIter.next();
				namespaceUri = edge.getTargetNode().getNodeUri();
				if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY && namespaceUri.contains(XMLNamespace))
						Attrs++;
			}//end while edgeFromIte.hasNext
		}//end it.hasNext
		return Attrs;
	}//end method2Phase1
	private void method2Phase2( String namespace_uri, Hashtable<String, ConnectivityInfo> nodeRanks){
		//Fraction Denominators
		int subCl = 0, supCl = 0, rangesCl = 0, domainsCl = 0;
		//Fraction Numerator
		int attrs = 0;
		ConnectivityInfo currNodeConnectInfo = null;
		//Node and edge objects required for obtaining temporal data
		Node node = null;
		Edge edge = null;
		//Iterators to scan through lists of edges (from and to, both located in each node) and the list of all graph nodes
		Iterator edgeFromIter = null, edgeToIter = null, nodeIt = null;

		String namespaceUri;
		//Calculate connectivity info for each node - classes(sub and sup) and properties (domains and ranges)
		nodeIt = nodeList.values().iterator();
		while(nodeIt.hasNext()){
			node = (Node)nodeIt.next();
			namespaceUri = node.getNodeUri();
			if(!
					(namespaceUri.contains(namespace_uri) ||
					 namespaceUri.contains(XMLNamespace) ||
					 namespaceUri.contains(RDFNamespace) ||
					 namespaceUri.contains(RDFSNamespace))
				)
				continue;
			//Create a connectivity info object that will correspond to this node
			currNodeConnectInfo = new ConnectivityInfo(node.getName(),1);
			//**************************************************************************************
			//Get all edges starting from this node
			edgeFromIter = node.getEdgesFrom().iterator();
			while(edgeFromIter.hasNext()){
				edge = (Edge)edgeFromIter.next();
				if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY){
					namespaceUri = edge.getTargetNode().getNodeUri();
					if(namespaceUri.contains(XMLNamespace))
						attrs++;
					else if(
							namespaceUri.contains(namespace_uri) ||
							namespaceUri.contains(RDFNamespace) ||
							namespaceUri.contains(RDFSNamespace)
							)
						domainsCl++;
				}
				else if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF)
					supCl++;
			}//end while edgeFromIter.hasNext
			//**************************************************************************************
			//Get all edges arriving to this node
			edgeToIter = node.getEdgesTo().iterator();
			while(edgeToIter.hasNext()){
				edge = (Edge)edgeToIter.next();
				if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF)
					subCl++;
				else if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY)
					rangesCl++;
			}//end while edgeToIter.hasNext
			//**************************************************************************************
			//Populate connectivity info object and add it to returning hashtable
			currNodeConnectInfo.setAttrs(attrs);
			currNodeConnectInfo.setSubclassesNo(subCl);
			currNodeConnectInfo.setSuperclassesNo(supCl);
			currNodeConnectInfo.setRangeclassesNo(rangesCl);
			currNodeConnectInfo.setDomainclassesNo(domainsCl);
			nodeRanks.put(currNodeConnectInfo.getName(), currNodeConnectInfo);
			//**************************************************************************************
			//Zero temporal numerators and denominators
			attrs = 0;
			subCl = 0;
			supCl = 0;
			rangesCl = 0;
			domainsCl = 0;
		}//end while nodeIt.hasNext
	}//end method2Phase2

	private void method2Phase3( String namespace_uri, Hashtable<String, ConnectivityInfo> nodeRanks, int Attrs){
		ConnectivityInfo currNodeConnectInfo = null;
		ConnectivityInfo edgeNodeConnectInfo = null;
		//Node and edge objects required for obtaining temporal data
		Node node = null;
		Edge edge = null;
		//Iterators to scan through lists of edges (from and to, both located in each node) and the list of all graph nodes
		Iterator edgeFromIter = null, edgeToIter = null, nodeIt = null;
		//Partial aggregators for each node
		double classRelatedSum = 0;
		double propertyRelatedSum = 0;
		double rank = 0;
		//
		String namespaceUri = "";
		//In each iteration, nodelist iterator should reset to head of object lis
		nodeIt = nodeList.values().iterator();
		while(nodeIt.hasNext()){
			node = (Node)nodeIt.next();
			namespaceUri = node.getNodeUri();
			if(!
					(namespaceUri.contains(namespace_uri) ||
					 namespaceUri.contains(XMLNamespace) ||
					 namespaceUri.contains(RDFNamespace) ||
					 namespaceUri.contains(RDFSNamespace))
				)
				continue;
			//Get connectivity info for current node from container
			currNodeConnectInfo = nodeRanks.get(node.getName());
			//**************************************************************************************
			//Get all edges starting from this node
			edgeFromIter = node.getEdgesFrom().iterator();
			while(edgeFromIter.hasNext()){
				edge = (Edge)edgeFromIter.next();
				//Get connectivity info for the target node from container
				edgeNodeConnectInfo = nodeRanks.get(edge.getTargetNode().getName());
				if(edgeNodeConnectInfo == null)
					continue;

				if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY){
					namespaceUri = edge.getTargetNode().getNodeUri();
					if(
						namespaceUri.contains(namespace_uri) ||
						namespaceUri.contains(RDFNamespace) ||
						namespaceUri.contains(RDFSNamespace)
						)
						if(edgeNodeConnectInfo.getRangeclassesNo() + edgeNodeConnectInfo.getDomainclassesNo() != 0)
							propertyRelatedSum += (double)edgeNodeConnectInfo.getPreviousRank()/(edgeNodeConnectInfo.getRangeclassesNo()+edgeNodeConnectInfo.getDomainclassesNo());
				}//end if edge.rdfObjectType == Property
				else if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF)
					if(edgeNodeConnectInfo.getSubclassesNo()+edgeNodeConnectInfo.getSuperclassesNo() != 0)
						classRelatedSum += (double)edgeNodeConnectInfo.getPreviousRank()/(edgeNodeConnectInfo.getSubclassesNo()+edgeNodeConnectInfo.getSuperclassesNo());
			}//end edgeFromIter.hasNext
			//**************************************************************************************
			//Get all edges arriving to this node
			edgeToIter = node.getEdgesTo().iterator();
			while(edgeToIter.hasNext()){
				edge = (Edge)edgeToIter.next();
				//Get connectivity info for the source node from container
				edgeNodeConnectInfo = nodeRanks.get(edge.getSourceNode().getName());
				if(edgeNodeConnectInfo == null)
					continue;

				if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.SUBCLASSOF)
					if(edgeNodeConnectInfo.getSubclassesNo()+edgeNodeConnectInfo.getSuperclassesNo() != 0)
						classRelatedSum += (double)edgeNodeConnectInfo.getPreviousRank()/(edgeNodeConnectInfo.getSubclassesNo()+edgeNodeConnectInfo.getSuperclassesNo());
				else if(edge.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY)
					if(edgeNodeConnectInfo.getRangeclassesNo()+edgeNodeConnectInfo.getDomainclassesNo() != 0)
						propertyRelatedSum += (double)edgeNodeConnectInfo.getPreviousRank()/(edgeNodeConnectInfo.getRangeclassesNo()+edgeNodeConnectInfo.getDomainclassesNo());
			}//end while edgesToIter.hasNext
			//
			/////////////////////////////////////////////////////////////////////////////////////////
			//
			//Calculate rank according to method2 type
			if(Attrs!=0)
				rank = ((double)(q1*((double)currNodeConnectInfo.getAttrs()/Attrs))) + ((double)q2*classRelatedSum) + ((double)q3*propertyRelatedSum);
			else
				rank = ((double)q2*classRelatedSum) + ((double)q3*propertyRelatedSum);
			//**************************************************************************************
			//Set rank for both node and connectivity info corresponding to it
			node.rankingScore = rank;
			currNodeConnectInfo.setRank(rank);
			System.out.println(node.getName()+ " : " +node.rankingScore);
			//Zero Partial aggregators
			classRelatedSum = 0;
			propertyRelatedSum = 0;
		}//end while nodeIt.hasNext
	}//end method2Phase3
	private boolean method2Phase4(Hashtable<String, ConnectivityInfo> nodeRanks, int loop){
		Iterator<ConnectivityInfo> nodeRanksIt = null;
		ConnectivityInfo currNodeConnectInfo = null;
		//If number of changes is less than 20% then stop iterations
		int changes = 0;

		nodeRanksIt = nodeRanks.values().iterator();
		while(nodeRanksIt.hasNext()){
			currNodeConnectInfo = (ConnectivityInfo)nodeRanksIt.next();
			if(currNodeConnectInfo.getPreviousRank() - currNodeConnectInfo.getRank() < 1E-5)
				changes++;
			currNodeConnectInfo.setPreviousRank(currNodeConnectInfo.getRank());
		}
		if(changes > (80*nodeRanks.size()/100)){
			System.out.println("No significant changes performed in iteration i: "+loop);
			bound = loop;
			return true;
		}//end if changes>80%
		return false;
	}
	/**
	 * Random Surfer Inferred
	 * Calculate node's rank using this formula
	 * @param model opened model
	 * @param namespace_uri chosen namespace
	 * @return a HashTable with ConnectivityInfo objects that holds all ranks
	 */


	/**
	 * Random Surfer Explicit
	 * Calculate node's rank using this formula
	 * @param namespace_uri chosen namespace
	 * @param q1 parameter for the ranking formula
	 * @param q2 parameter for the ranking formula
	 * @param q3 parameter for the ranking formula
	 * @return a HashTable with ConnectivityInfo objects that holds all ranks
	 */
	public Hashtable<String, ConnectivityInfo> method2( String namespace_uri,double q1, double q2, double q3){
		int Attrs = 0;
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;

		//This hashtable will be returned populated for later use in score file generation functions
		Hashtable<String, ConnectivityInfo> nodeRanks = null;
		//Allocate memory for the exact number of nodes in this graph
		nodeRanks = new Hashtable<String, ConnectivityInfo>(nodeList.size());
		//**************************************************************************************
		//Phase 1
		//Calculate the graph's total number of attrs
		Attrs = method2Phase1();
		method2Phase2(namespace_uri, nodeRanks);
		for(int loop=0;loop<100;loop++){
			method2Phase3(namespace_uri, nodeRanks, Attrs);
			if(method2Phase4(nodeRanks, loop))
				break;
		}//end for loop < 100
		//
		//**************************************************************************************
		generateSortedTopK();
	    return nodeRanks;
	}//end method2
	public Hashtable<String, ConnectivityInfo> method5(String namespace_uri){
		Iterator nodeIt = null, propIt = null, domainIt = null, rangeIt = null, allSuperClassIt;
		Hashtable<String, ConnectivityInfo> nodeRanks = null;
		ConnectivityInfo currNodeConnectInfo, superclNodeConnectInfo = null;
		Node node = null, superclassNode = null;
		String domainName;
		String rangeName;

		int rank = 0;

		//**************************************************************************************
		//Allocate memory for returning hashtable
		nodeRanks = new Hashtable<String, ConnectivityInfo>(nodeList.size());
		//Allocate memory for each node object in connectivity info hashtable
		nodeIt = nodeList.values().iterator();
		while(nodeIt.hasNext()){
			node = (Node)nodeIt.next();
			currNodeConnectInfo = new ConnectivityInfo(node.getName(),0);
			nodeRanks.put(node.getName(), currNodeConnectInfo);
		}//end nodeIt.hasNext
		//**************************************************************************************
//		//Get model namespace in order to retrieve all its properties
//		ns = model.getNamespace(namespace_uri);
//		propIt = ns.getProperties().iterator();
//		while(propIt.hasNext()){
//			RDFProperty p = (RDFProperty)propIt.next();
//
//			domainIt = p.getDomains().iterator();
//			rangeIt = p.getRanges().iterator();
//			if(domainIt.hasNext()){
//					domainName = ((RDFClass)domainIt.next()).getLocalName();
//					currNodeConnectInfo = nodeRanks.get(domainName);
//					currNodeConnectInfo.setPreviousRank(currNodeConnectInfo.getPreviousRank() + 1);
//			}//if domainsIter.hasNext
//			if(rangeIt.hasNext()){
//					rangeName = ((RDFClass)rangeIt.next()).getLocalName();
//					currNodeConnectInfo = nodeRanks.get(rangeName);
//					currNodeConnectInfo.setPreviousRank(currNodeConnectInfo.getPreviousRank() + 1);
//			}//if rangesIter.hasNext
//		}//end while propIt.hasNext
                propIt = edgeList.iterator();
                while(propIt.hasNext()){
                    Edge property = (Edge) propIt.next();
                    if(property.rdfObjectType == SEMWEB_OBJECT_TYPE.PROPERTY){
                        currNodeConnectInfo = nodeRanks.get(property.getSourceNode().getName());
                        currNodeConnectInfo.setPreviousRank(currNodeConnectInfo.getPreviousRank() + 1);
                        currNodeConnectInfo = nodeRanks.get(property.getTargetNode().getName());
                        currNodeConnectInfo.setPreviousRank(currNodeConnectInfo.getPreviousRank() + 1);
                    }
                }
		//**************************************************************************************
		//Calculate rank for each node
		nodeIt = nodeList.values().iterator();
		while(nodeIt.hasNext()){
			rank = 0;
			node = (Node)nodeIt.next();
			//Set all needed variables in connectivity info object for this node
			currNodeConnectInfo = nodeRanks.get(node.getName());
                        currNodeConnectInfo.setAllSuperclassesNo(node.getSuperClasses(false).size());
                        currNodeConnectInfo.setAllSubclassesNo(node.getSubClasses(false).size());
			//Increase previous rank for each superclass of current node
			allSuperClassIt = node.getSuperClasses(false).iterator();
			while(allSuperClassIt.hasNext()){
				superclassNode = nodeList.get(((Node)allSuperClassIt.next()).getName());
				if(superclassNode != null){
					superclNodeConnectInfo = nodeRanks.get(superclassNode.getName());
					rank += superclNodeConnectInfo.getPreviousRank();
				}
			}//end while
			//**************************************************************************************
			rank += (currNodeConnectInfo.getPreviousRank() + currNodeConnectInfo.getAllSubclassesNo() + currNodeConnectInfo.getAllSuperclassesNo());
			node.rankingScore += rank;
			currNodeConnectInfo.setRank(rank);
		}//end while nodeIt.hasNext
		generateSortedTopK();
		return nodeRanks;
	}//end method5


	/**
	 * Calculate ranks for all nodes using chosen methods and generate the scores file that contains results
	 * @param fileName file name to save scores
	 * @param params params used by chosen method(s)
	 * @param methods chosen method(s) identifier(s)
	 * @param name schema name
	 * @param namespace_uri chosen namespace
	 * @return true upon successful file generation, false otherwise
	 */
	public boolean generateScoreFile(String fileName, String params, String methods, String name, String namespace_uri){

		Hashtable<String, ConnectivityInfo> method0Objs = null;
		Hashtable<String, ConnectivityInfo> method2Objs = null;
		Hashtable<String, ConnectivityInfo> method5Objs = null;

		boolean method0 = false, method2 = false, method5 = false;
		boolean method0HasIt = false, method2HasIt = false, method5HasIt = false;

		Iterator<ConnectivityInfo> mainIt = null;

		if(!params.equals("")){
			q1 = Double.parseDouble(params.split(",")[0]);
			q2 = Double.parseDouble(params.split(",")[1]);
			q3 = Double.parseDouble(params.split(",")[2]);

			if(q1+q2+q3 != 1)
				return false;
		}

		if(methods.contains("method0")){
			method0Objs = method0(namespace_uri);
			method0 = true;
		}
		if(methods.contains("method2")){
			method2Objs = method2(namespace_uri,q1,q2,q3);
			method2 = true;
		}
		if(methods.contains("method5")){
			method5Objs = method5(namespace_uri);
			method5 = true;
		}

		ConnectivityInfo conTmp = null;

		Calendar cal = new GregorianCalendar();

	    // Get the components of the time
	    int hour24 = cal.get(Calendar.HOUR_OF_DAY);		// 0..23
	    int min = cal.get(Calendar.MINUTE);            	// 0..59
	    int sec = cal.get(Calendar.SECOND);             // 0..59

	    try{
	    	BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
	    	if(method0){
	    		mainIt = method0Objs.values().iterator();
	    		method0HasIt = true;
	    	}
	    	else if(method2){
	    		mainIt = method2Objs.values().iterator();
	    		method2HasIt = true;
	    	}
	    	else if(method5){
	    		mainIt = method5Objs.values().iterator();
	    		method5HasIt = true;
	    	}

	    	out.write("%schema: "+name+ "\n");
			out.write("%time: " +hour24+ ":" +min+ ":" +sec+ "\n");
			out.write("%Parameters used in M2 : q1=" +q1+ ", q2=" +q2+ ", q3=" +q3+ "\n");
			out.write("%Boundary used in M2 : 80% of total nodes' ranks changed less than 10^-5 from last iteration\n");
			out.write("%Loops performed : " +bound+ "\n");
			out.write("%\n%\n");
			out.write("%\t   Method\n");

			//**************************************************************************************
			//Write down to file the appropriate method identifiers
			out.write("%Class ");
			if(method0)
				out.write("-- M0 ");
			if(method2)
				out.write("-- M2 ");
			if(method5)
				out.write("-- M5 ");
			out.write("\n");
			//**************************************************************************************
			//For each method write down the results for all nodes
			while(mainIt.hasNext()){
				conTmp = mainIt.next();
				out.write(conTmp.getName()+ " -- " +conTmp.getRank());
				if(method0 && !method0HasIt)
					out.write(" -- " +method0Objs.get(conTmp.getName()).getRank());
				if(method2 && !method2HasIt)
					out.write(" -- " +method2Objs.get(conTmp.getName()).getRank());
				if(method5 && !method5HasIt)
					out.write(" -- " +method5Objs.get(conTmp.getName()).getRank());
				out.write("\n");
			}
	        out.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    return true;
	}//end generateScoreFile

    /**
	 * Generate sorted topKnodes array using node's rank as comparator
	 */
	private void generateSortedTopK(){
		rankedNodes = nodeList.values().toArray();
		Arrays.sort(rankedNodes,new rankComparator());
	}//end generateSortedTopK



}

/**
 * Custom comparator for node objects which uses node's ranking score member
 * @author leonidis
 *
 */
class rankComparator implements Comparator<Object>{

	/**
	 * Method that compares two objects by comparing their ranking score values
	 */
	public int compare(Object o1, Object o2) {
		if(((Node)o1).rankingScore < ((Node)o2).rankingScore)
			return -1;
		else if(((Node)o1).rankingScore > ((Node)o2).rankingScore)
			return 1;
		else
			return 0;
	}
}//end class myComparator
