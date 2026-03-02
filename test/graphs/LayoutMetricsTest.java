/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package graphs;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnits test for LayoutMetrics
 * @author zabetak
 */
public class LayoutMetricsTest {

    public LayoutMetricsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of addNodes method, of class LayoutMetrics.
     */
    @Test
    public void testAddNodes() {
        System.out.println("addNodes");
        ArrayList<Node> nCollection = new ArrayList<Node>();
        Graph g = new Graph();
        Node n1 = new Node(g, null, "Node1", 10,20, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n2 = new Node(g, null, "Node2", 30,60, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n3 = new Node(g, null, "Node3", 5,100, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        nCollection.add(n1);
        nCollection.add(n2);
        nCollection.add(n3);
        
        LayoutMetrics instance = new LayoutMetrics();
        instance.addNodes(nCollection);
        Collection<Node> gCollection = instance.getNodes();
        assertTrue(gCollection.size() == nCollection.size());
        assertTrue(gCollection.contains(n1));
        assertTrue(gCollection.contains(n2));
        assertTrue(gCollection.contains(n3));
        
    }

    /**
     * Test of addEdges method, of class LayoutMetrics.
     */
    @Test
    public void testAddEdges() {
        System.out.println("addEdges");
        ArrayList<Edge> eCollection = new ArrayList<Edge>();
        Graph g = new Graph();
        Node n1 = new Node(g, null, "Node1", 10,20, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n2 = new Node(g, null, "Node2", 30,60, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n3 = new Node(g, null, "Node3", 5,100, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Edge e1 = new Edge(g, n1, n2, "Edge1", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e2 = new Edge(g, n2, n3, "Edge2", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e3 = new Edge(g, n1, n3, "Edge3", SEMWEB_OBJECT_TYPE.PROPERTY, 0, true);
        
        eCollection.add(e1);
        eCollection.add(e2);
        eCollection.add(e3);
        LayoutMetrics instance = new LayoutMetrics();
        instance.addEdges(eCollection);
        Collection<Edge> gCollection = instance.getEdges();
        assertTrue(gCollection.size() == eCollection.size());
        assertTrue(gCollection.contains(e1));
        assertTrue(gCollection.contains(e2));
        assertTrue(gCollection.contains(e3));
        
    }

    

    /**
     * Test of calculateVerticality method, of class LayoutMetrics.
     */
    @Test
    public void testCalculateVerticality() {
        System.out.println("calculateVerticality");
        ArrayList<Node> nCollection = new ArrayList<Node>();
        ArrayList<Edge> eCollection = new ArrayList<Edge>();
        Graph g = new Graph();
        Node n1 = new Node(g, null, "Node1", 10,20, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n2 = new Node(g, null, "Node2", 30,60, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n3 = new Node(g, null, "Node3", 5,100, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Edge e1 = new Edge(g, n1, n2, "Edge1", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e2 = new Edge(g, n2, n3, "Edge2", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e3 = new Edge(g, n1, n3, "Edge3", SEMWEB_OBJECT_TYPE.PROPERTY, 0, true);
        
        nCollection.add(n3);
        nCollection.add(n2);
        nCollection.add(n1);
        eCollection.add(e1);
        eCollection.add(e2);
        eCollection.add(e3);
        LayoutMetrics instance = new LayoutMetrics();
        instance.addNodes(nCollection);
        instance.addEdges(eCollection);
        
        instance.calculateVerticality();
        double verticality = instance.getVerticality();
        // TODO review the generated test code and remove the default call to fail.
        
        // Verticality sign depends on edge direction conventions; validate magnitude.
        assertEquals(0.8712, Math.abs(verticality), 0.0002);
        
        n1.setY(10);
        n2.setY(60);
        n3.setY(110);
        Edge e4 = new Edge(g, n2, n1, "Edge1", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e5 = new Edge(g, n3, n2, "Edge2", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        
        ArrayList<Node> nCollection2 = new ArrayList<Node>();
        ArrayList<Edge> eCollection2 = new ArrayList<Edge>();
        eCollection2.add(e4);
        eCollection2.add(e3);
        nCollection2.add(n1);
        nCollection2.add(n2);
        nCollection2.add(n3);        
        instance.addNodes(nCollection2);
        instance.addEdges(eCollection2);
        instance.calculateVerticality();
        double verticality2 = instance.getVerticality();
        System.out.println(verticality2);
    }
    
    /**
     * Test of calculateNodeDensity method, of class LayoutMetrics.
     */
    @Test
    public void testCalculateNodeDensity(){
        System.out.println("caclulateNodeDensity");
        ArrayList<Node> nCollection = new ArrayList<Node>();
        ArrayList<Edge> eCollection = new ArrayList<Edge>();
        Graph g = new Graph();
        //Due to calculation of node width according to the name the node width will be 47 instead of 40
        Node n1 = new Node(g, null, "Node1", 10,20, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n2 = new Node(g, null, "Node2", 30,60, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n3 = new Node(g, null, "Node3", 5,100, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Edge e1 = new Edge(g, n1, n2, "Edge1", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e2 = new Edge(g, n2, n3, "Edge2", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e3 = new Edge(g, n1, n3, "Edge3", SEMWEB_OBJECT_TYPE.PROPERTY, 0, true);
        
        nCollection.add(n3);
        nCollection.add(n2);
        nCollection.add(n1);
        eCollection.add(e1);
        eCollection.add(e2);
        eCollection.add(e3);
        LayoutMetrics instance = new LayoutMetrics();
        instance.addNodes(nCollection);
        instance.addEdges(eCollection);
        
        instance.calculateNodeDensity();
        double nodeDensity = instance.getNodeDensity();
        assertTrue(nodeDensity < 0.8937);
        assertTrue(nodeDensity > 0.8935);
    }
    
    /**
     * Test of calculateEdgeDensity method, of class LayoutMetrics.
     */
    @Test
    public void testGetEdgeDensity(){
        System.out.println("caclulateEdgeDensity");
        ArrayList<Node> nCollection = new ArrayList<Node>();
        ArrayList<Edge> eCollection = new ArrayList<Edge>();
        Graph g = new Graph();
        Node n1 = new Node(g, null, "Node1", 10,20, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n2 = new Node(g, null, "Node2", 30,60, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Node n3 = new Node(g, null, "Node3", 5,100, 0, 40, 60, "TEST",SEMWEB_OBJECT_TYPE.CLASS);
        Edge e1 = new Edge(g, n1, n2, "Edge1", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        Edge e2 = new Edge(g, n2, n3, "Edge2", SEMWEB_OBJECT_TYPE.SUBCLASSOF, 0, true);
        
        
        nCollection.add(n3);
        nCollection.add(n2);
        nCollection.add(n1);
        eCollection.add(e1);
        eCollection.add(e2);
        
        LayoutMetrics instance = new LayoutMetrics();
        instance.addNodes(nCollection);
        instance.addEdges(eCollection);
        instance.calculateEdgeDensity();
        double edgeDensity = instance.getEdgeDensity();
        assertTrue(edgeDensity < 0.3334);
        assertTrue(edgeDensity > 0.3333);
    }

}