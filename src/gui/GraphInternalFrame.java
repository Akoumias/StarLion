/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import controller.GraphController;
import gr.forth.ics.rdfsuite.services.RdfDocument;
import gr.forth.ics.swkmclient.Client;
import graphs.Graph;
import graphs.GraphNode;
import graphs.Node;
import gui.MenuActions.StarViewDefaultAction;
import gui.Panels.ForcesPanel;
import gui.Panels.InstancesListPanel;
import gui.Panels.RigthClick.PropertiesPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import mapping.GraphBackendMode;
import mapping.ParityComparator;
import mapping.ParityReport;
import mapping.adapter.JenaCanonicalAdapter;
import mapping.adapter.SwkmCanonicalAdapter;
import mapping.canonical.CanonicalGraphSnapshot;
import mapping.canonical.CanonicalRdfSnapshot;
import model.RDFModel;


/**
 *
 * @author zabetak
 */
public class GraphInternalFrame extends InternalFrame{
    
    private GraphController graphController;
    private Graph       graph = null;
    private JPopupMenu 	popup;
    private JScrollPane     spane = null;
    private JToolBar    toolBar = null;
    private int globalTimesTEST = 0;
    /*Debug and Test Fields for metrics*/    
//    private JTextField  a_field = new JTextField("a");
//    private JTextField  b_field = new JTextField("b");
//    private JTextField  vert_field = new JTextField("Verticality");
//    private JTextField  nd_field = new JTextField("Node Density");
//    private JTextField  ed_field = new JTextField("Edge Density");
//    private JTextField  nodes_field = new JTextField("N");
//    private JTextField  edges_field = new JTextField("E");
    
    
    
    
    public GraphInternalFrame(String name, Project parent){
        super(name,parent);
        this.graphController = new GraphController();
        this.graph = graphController.getGraphService().getGraph();

        //Add popup window facilitate	
		popup = new JPopupMenu();
		JMenuItem nail = new JMenuItem("Nail Selected Node(s)");
                JMenuItem unail = new JMenuItem("UNail Selected Node(s)");
                JMenuItem hide = new JMenuItem("Hide Selected Node(s)");
                JMenuItem allNSpaceNodes = new JMenuItem("Select the node's namespace");
                
                JMenuItem unailAll = new JMenuItem("UNail all Nodes");
                JMenuItem unhideAll = new JMenuItem("Show All Hidden Nodes");
                JMenuItem nSpace = new JMenuItem("Open Node's Namespace");
                JMenuItem properties = new JMenuItem("Properties");
                JMenuItem forces = new JMenuItem("Show Forces");
                JMenuItem getInstances = new JMenuItem("Get Instances");
                
		//Add listener in order to provide hiding nodes
		hide.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
            		getGraph().hideSelectedNodes();
                    }//end actionPerformed
                }
		);
                
                unhideAll.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
            		getGraph().makeNodesVisible();
                    }//end actionPerformed
                }
		);
                
                
                nail.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        getGraph().nailSelectedNodes(true);
                    }//end actionPerformed
                });
                
                unail.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        getGraph().nailSelectedNodes(false);
                    }
                });
                
                unailAll.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        getGraph().makeNodesUnailed();
                    }
                });
                
                getInstances.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
//                        System.out.println("Instance Retrieval");
//                        System.out.println("------------------");
//                        for(String s:getGraph().getInstances(getGraph().getSelectedNodeName())){
//                            System.out.println(s);
//                        }
                        //Create objects that will appear in OptionPane
                        Object[] message = new Object[1];
                        //Create Button names
                        String[] instances = getGraph().getInstances(getGraph().getSelectedNodes()[0].getName());
                        InstancesListPanel ilp = new InstancesListPanel(instances);
                        message[0] = ilp;
                        String[] options = {"View","Cancell"};
                        int result = JOptionPane.showOptionDialog( 
                            null,                     					// the parent that the dialog blocks 
	 		    message,                                           		// the dialog message array 
	 		    "Instances",                                                   // the title of the dialog window 
	 		    JOptionPane.DEFAULT_OPTION,                                 // option type 
	 		    JOptionPane.PLAIN_MESSAGE,                                  // message type 
	 		    null,                                                       // optional icon, use null to use the default icon 
	 		    options,                                   			// options string array, will be made into buttons 
	 		    options[0]                                  		// option that should be made into a default button 
                        );
                        if(result == 0){
                            String instance = ilp.ok();
                            System.out.println("Selected Instance="+instance);
                            Client c = ProjectManager.getSingleton().getActiveProject().getActiveClient();
                            getGraph().addInstanceAndNeighbours(instance, c);
                            getGraph().setSelectedNode(instance);
                            StarViewDefaultAction svda = new StarViewDefaultAction();
                            svda.execute();
                            
                        }
//                            if(globalTimesTEST == 0){
//                            getGraph().instanceView("TestInstance1");
//                            globalTimesTEST++;
//                            }else{
//                            getGraph().instanceView("N1");
//                          }
//                        }
                        
                    }
                });
                /*
                 * This rigth click action takes the namespace of the selected node
                 * and if this namespace is already opened in an existing frame then this 
                 * frame is highlighted else the namespace is opened in a new frame
                 */
                
                nSpace.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        //if no node is selected do nothing
                        
                        if(getGraphSelectedNodes() == 0)
                              return;
                        String[] selNodesNspace = getGraph().getSelectedNodeNamespaces();
                       //If node's namespace exist already in the same frame
                       if(getGraph().nodeNSexistINGraph(selNodesNspace)){
                           return;
                       }else{
                            
                           int totalFrames = ProjectManager.getSingleton().getActiveProject().getTotalFramesNo();
                           
                           boolean frameFound = false;
                           //Search all frames to check if the namespace of the selected node exists anywhere
                           for(int i =0;i < totalFrames;i++){
                               
                               GraphInternalFrame iFrame = (GraphInternalFrame) ProjectManager.getSingleton().getActiveProject().getFrameAt(i);
                               boolean namespaceExist = iFrame.getGraph().nodeNSexistINGraph(selNodesNspace);
                               if(namespaceExist){
                                   
                                   ProjectManager.getSingleton().getActiveProject().setActiveFrame(iFrame);
                                   ProjectTree.getSingleton().highlighActiveFrame(ProjectManager.getSingleton().getActiveProject(), iFrame);
                                   frameFound = true;
                                   break;
                               }
                           }
                           //The namespace of the selected node isn't open so we must create a new frame
                           if(!frameFound){
                               System.out.println("Trith Periptwsh");
//                               MainFrame.getSingleton().getMenubar().newVisualizationFrame(getGraph().getSelectedNodeNamespaces(), false);
                               
                               ProjectManager.getSingleton().getActiveProject().addVisualizationFrame("frame" +ProjectManager.getSingleton().getActiveProject().getFrameCounter());
                               ((GraphInternalFrame)ProjectManager.getSingleton().getActiveProject().getActiveFrame()).getGraph().setGraphNameSpaces(getGraph().getSelectedNodeNamespaces());
//                               ((GraphInternalFrame)ProjectManager.getSingleton().getActiveProject().getActiveFrame()).getGraph().setGraphExternalLinks(true);
                               ProjectManager.getSingleton().getActiveProject().addGraphInFrame("", "");
                                MainFrame.getSingleton().getDesktop().add(ProjectManager.getSingleton().getActiveProject().getActiveFrame());
                                ProjectTree.getSingleton().highlighActiveFrame(ProjectManager.getSingleton().getActiveProject(),ProjectManager.getSingleton().getActiveProject().getActiveFrame());
                                
                           }
                       }
                       
                    }
                });
                
                allNSpaceNodes.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        //if no node is selected do nothing
                        if(getGraphSelectedNodes() == 0)
                              return;
                        getGraph().getAllNodesOfNamespace(getGraph().getSelectedNodeNamespaces()[0]);
                    }
                });
                
               forces.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        //getGraph().forceDirectedPlacement(null, false);
                        String nodeForces[] = getGraph().getSelectedNodeForces().split(" ");
//                        int forceX = (int)Double.parseDouble(nodeForces[0]);
//                        int forceY = (int)Double.parseDouble(nodeForces[1]);
                        int startX = Integer.parseInt(nodeForces[2]);
                        int startY = Integer.parseInt(nodeForces[3]);
                        int endX = Integer.parseInt(nodeForces[4]);
                        int endY = Integer.parseInt(nodeForces[5]);
                        
//                        double forceLength = Math.sqrt(Math.pow((startX-endX),2.0)+Math.pow((startY-endY),2.0));
                        double forceLength = Math.sqrt(Math.pow((endX-startX),2.0)+Math.pow((endY-startY),2.0));
                        DecimalFormat dFormat = new DecimalFormat("##.###");
                        //Create objects that will appear in OptionPane
                        Object[] message = new Object[1];
                        //Create Button names
                        String[] options = {"OK"};
                        //Add to Optionpane all previously created panels
                        JPanel jp = new JPanel();
                        BoxLayout bl = new BoxLayout(jp, BoxLayout.Y_AXIS);
                        jp.setLayout(bl);
                        System.out.println("Forces:"+getGraph().getSelectedNodeForces());
                        ForcesPanel fpp = new ForcesPanel(startX,startY,endX,endY);
//                        ForcesPanel fpp = new ForcesPanel(endX,endY,forceX,forceY);
                        
                        JPanel powerP = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        JLabel powerL = new JLabel("Power:");
                        JTextField powerF = new JTextField(dFormat.format(forceLength));

                        
                        powerP.add(powerL);
                        powerP.add(powerF);
                        

                        jp.add(fpp);
                        jp.add(powerP);
                        message[0] = jp;
                        
                        int result = JOptionPane.showOptionDialog( 
                            null,                     					// the parent that the dialog blocks 
	 		    message,                                           		// the dialog message array 
	 		    "Forces",                                                   // the title of the dialog window 
	 		    JOptionPane.DEFAULT_OPTION,                                 // option type 
	 		    JOptionPane.PLAIN_MESSAGE,                                  // message type 
	 		    null,                                                       // optional icon, use null to use the default icon 
	 		    options,                                   			// options string array, will be made into buttons 
	 		    options[0]                                  		// option that should be made into a default button 
                        );
                        
                    }
                });
                
                properties.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        //if no node is selected do nothing
                        if(getGraphSelectedNodes() == 0)
                              return;
                        String []nodesNameSpace =getGraph().getSelectedNodeNamespaces();
                        Node cNode = getGraph().getSelectedNodes()[0];
                        GraphNode sNode = cNode.getGraphNode();
                        String x = String.valueOf(sNode.getXposition());
                        String y = String.valueOf(sNode.getYposition());
                        String h = String.valueOf(sNode.getNodeHeigth());
                        String w = String.valueOf(sNode.getNodeWidth());
                        Boolean nailedSt = cNode.isNailed();
                        //Create the main panel
                        JPanel propPanel = new PropertiesPanel(nodesNameSpace[0],x,y,w,h,nailedSt);
                        
                        //Create objects that will appear in OptionPane
                        Object[] message = new Object[1];
                        //Create Button names
                        String[] options = {"OK"};
                        //Add to Optionpane all previously created panels
                        message[0] = propPanel;
    	
                        int result = JOptionPane.showOptionDialog( 
                            null,                     					// the parent that the dialog blocks 
	 		    message,                                           		// the dialog message array 
	 		    "Node's Properties",                       			// the title of the dialog window
	 		    JOptionPane.DEFAULT_OPTION,                                 // option type 
	 		    JOptionPane.INFORMATION_MESSAGE,                            // message type 
	 		    null,                                                       // optional icon, use null to use the default icon 
	 		    options,                                   			// options string array, will be made into buttons 
	 		    options[0]                                  		// option that should be made into a default button 
                        );
                    }
                });
                
                popup.add(nail);
                popup.add(unail);
		popup.add(hide);
                popup.add(nSpace);
                popup.add(allNSpaceNodes);
                popup.add(properties);
                popup.add(forces);
                popup.addSeparator();
                popup.add(unailAll);
		popup.add(unhideAll);
                popup.add(getInstances);
                
                toolBar = new JToolBar("Modify Graph");
                
                
                ImageIcon plus_icon = createImageIcon("/config/plus.gif", "plus icon");
                ImageIcon minus_icon = createImageIcon("/config/minus.gif", "minus icon");
                ImageIcon magicB_icon = createImageIcon("/config/mb.gif", "mb icon");
                
                JButton plus_subClassVerticality = new JButton(plus_icon);
                JButton minus_subClassVerticality = new JButton(minus_icon);
                JButton plus_nodeRepulsion = new JButton(plus_icon);
                JButton minus_nodeRepulsion = new JButton(minus_icon);
                JButton plus_springLength = new JButton(plus_icon);
                JButton minus_springLength = new JButton(minus_icon);
                JButton plus_zoom = new JButton(plus_icon);
                JButton minus_zoom = new JButton(minus_icon);
                
                JCheckBox smooth_transi = new JCheckBox("Smooth Transitions");
                smooth_transi.setSelected(true);
                
                JCheckBox edgeLabels = new JCheckBox("Edge Labels");
                edgeLabels.setSelected(true);
                /*TO BE IMPLEMENTED
                JCheckBox propertieEdges = new JCheckBox("Propertie Edges");
                propertieEdges.setSelected(true);
                
                JCheckBox isAEdges = new JCheckBox("isA Edges");
                isAEdges.setSelected(true);
                */
                JButton magic_button = new JButton(magicB_icon);
                
                //FOR TESTING METRICS
//                
//                JButton vcorrect_button = new JButton("VerticalityC");
//                JButton dcorrect_button = new JButton("DensityC");
//                JButton scaleC_button = new JButton("ScaleC");
//                
//                JButton vert_b = new JButton("Get V");
//                JButton nd_b = new JButton("Get ND");
//                JButton ed_b = new JButton("Get ED");
                //////////////////////////////
                JButton autoActions = new JButton("AutoActions");
                
                JLabel subClassVerticalityPA = new JLabel("SubClassOf");
                JLabel subClassVerticalityPB = new JLabel("Verticality");
                
                JLabel nodeRepulsionPA = new JLabel("Node");
                JLabel nodeRepulsionPB = new JLabel("Repulsion");
                
                JLabel springLengthPA  = new JLabel("Spring");
                JLabel springLengthPB  = new JLabel("Length");
                
                JLabel zoom          = new JLabel("Zoom");
                
                JLabel magic_buttonPA = new JLabel("Magic");
                JLabel magic_buttonPB = new JLabel("Button");
                
                JPanel panel1 = new JPanel();
                
                JPanel panel2 = new JPanel(new GridLayout(2,0));
                JPanel panel22 = new JPanel(new GridLayout(2,0));
                
                JPanel panel3 = new JPanel(new GridLayout(2,0));
                JPanel panel33 = new JPanel(new GridLayout(2,0));
                
                JPanel panel4 = new JPanel(new GridLayout(2,0));
                JPanel panel44 = new JPanel(new GridLayout(2,0));
                
                JPanel panel5 = new JPanel(new GridLayout(2,0));
                JPanel panel6 = new JPanel(new GridLayout(2,0));
                
                JPanel panel7 = new JPanel(new GridLayout(1,0));
                JPanel panel77 = new JPanel(new GridLayout(2,0));
                
                //FOR TESTING METRICS
//                JPanel paneltest = new JPanel(new GridLayout(5,0));
//                JPanel panel8 = new JPanel(new GridLayout(4,2));
                 
                //////////////////////////
                panel2.add(plus_subClassVerticality);
                panel2.add(minus_subClassVerticality);
                panel22.add(subClassVerticalityPA);
                panel22.add(subClassVerticalityPB);
                
                panel3.add(plus_nodeRepulsion);
                panel3.add(minus_nodeRepulsion);
                panel33.add(nodeRepulsionPA);
                panel33.add(nodeRepulsionPB);
                
                panel4.add(plus_springLength);
                panel4.add(minus_springLength);
                panel44.add(springLengthPA);
                panel44.add(springLengthPB);
                
                panel7.add(magic_button);
                //panel7.add(autoActions);
                panel77.add(magic_buttonPA);
                panel77.add(magic_buttonPB);
                
                panel5.add(plus_zoom);
                panel5.add(minus_zoom);
                panel6.add(smooth_transi);
                panel6.add(edgeLabels);
                /*TO BE IMPLEMENTED
                panel6.add(propertieEdges);
                panel6.add(isAEdges);
                */
                
                //FOR TESTING METRICS
//                paneltest.add(vcorrect_button);
//                paneltest.add(a_field);
//                paneltest.add(b_field);
//                paneltest.add(dcorrect_button);
//                paneltest.add(scaleC_button);
//                
//                panel8.add(vert_b);
//                panel8.add(vert_field);
//                panel8.add(nd_b);
//                panel8.add(nd_field);
//                panel8.add(ed_b);
//                panel8.add(ed_field);
//                panel8.add(nodes_field);
//                panel8.add(edges_field);
                 
                ////////////////////////////
                panel1.add(panel2);
                panel1.add(panel22);
                panel1.add(panel3);
                panel1.add(panel33);
                panel1.add(panel4);
                panel1.add(panel44);
                
                panel1.add(panel5);
                panel1.add(zoom);
                panel1.add(panel7);
                panel1.add(panel77);
                panel1.add(panel6);
                
                //FOR TESTING METRICS
//                panel1.add(paneltest);
//                panel1.add(panel8);
                 
                plus_subClassVerticality.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if( Double.isInfinite(graph.getKm()*2)){
                            System.out.println("OVERFLOW KM:SubClass Verticallity");
                            
                            
                        }else{
                            graph.changeFDPAparam("Km", graph.getKm()*2);
                            graph.updateLayout("forceDirected", null, null);
                            

                            //graph.forceDirectedPlacement(null);
                        }
                    };
                });
                
                minus_subClassVerticality.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getKm()/2 == 0.0){
                            System.out.println("N_OVERFLOW Km:SubClass Verticallity");
                        }else{
                            graph.changeFDPAparam("Km", graph.getKm()/2);
                            graph.updateLayout("forceDirected", null, null);
                            System.out.println(Double.MIN_VALUE);
                        }
                    }
                });
                
                plus_nodeRepulsion.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(Double.isInfinite(graph.getKe()*2)){
                            System.out.println("OVERFLOW Ke:Node Repulsion");
                        }else{
                            graph.changeFDPAparam("Ke", graph.getKe()*2);
                            graph.updateLayout("forceDirected", null, null);
                        }
                    }
                });
                
                minus_nodeRepulsion.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getKe()/2 == 0.0){
                            System.out.println("N_OVERFLOW Ke:Node Repulsion");
                        }else{
                            graph.changeFDPAparam("Ke", graph.getKe()/2);
                            graph.updateLayout("forceDirected", null, null);
                        }
                    }
                });
                
                plus_springLength.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(Double.isInfinite(graph.getLs() + 10)){
                            System.out.println("OVERFLOW Ls:Spring Length");
                        }else{
                            graph.changeFDPAparam("Ls", graph.getLs()+10);
                            graph.updateLayout("forceDirected", null, null);
                        }
                    }
                });
                
                minus_springLength.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getLs()-10 <= 0){
                            graph.changeFDPAparam("Ls",10);
                        }else{
                            graph.changeFDPAparam("Ls", graph.getLs()-10);
                            graph.updateLayout("forceDirected", null, null);
                        }
                    }
                });
                
                toolBar.add(panel1);
                JScrollPane toolbarScroll = new JScrollPane(toolBar);
                add(toolbarScroll,BorderLayout.SOUTH);
                
                plus_zoom.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int scaleFactor = graph.getScaleFactor();
                        scaleFactor += 10;
                        if(scaleFactor > 200){
                            graph.scale(200);
                        }else{
                            graph.scale(scaleFactor);
                        }
                        
                        
                    };
                });
                
                minus_zoom.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int scaleFactor = graph.getScaleFactor();
                        scaleFactor -= 10;
                        if(scaleFactor < 10){
                            graph.scale(10);
                        }else{
                            graph.scale(scaleFactor);
                        }
                        
                    };
                });
                
                smooth_transi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getSmoothTransitionsStatus()){
                            graph.setSmoothTransitions(false);
                        }else{
                            graph.setSmoothTransitions(true);
                        }
                    };
                });
                
                edgeLabels.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getEdgesLabelsVisibility()){
                            graph.setEdgeLabelsVisibility(false);
                            graph.getVisualGraph().updateGraph();
                        }else{
                            graph.setEdgeLabelsVisibility(true);
                            graph.getVisualGraph().updateGraph();
                        }
                    };
                });
                /*TO BE IMPLEMENTED
                propertieEdges.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getPropertieEdgesVisibility()){
                            graph.setPropertieEdgesVisibility(false);
                            graph.getVisualGraph().updateGraph();
                        }else{
                            graph.setPropertieEdgesVisibility(true);
                            graph.getVisualGraph().updateGraph();
                        }
                    };
                });
                
                isAEdges.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(graph.getisAEdgesVisibility()){
                            graph.setisAEdgesVisibility(false);
                            graph.getVisualGraph().updateGraph();
                        }else{
                            graph.setisAEdgesVisibility(true);
                            graph.getVisualGraph().updateGraph();
                        }
                    };
                });
                */
                magic_button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
//                        graph.correctingActions();
//                        graph.improveBoth();
                        graph.applyMagicCorrection("Both");
                    };
                });
                
                autoActions.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
//                try {
//                    graph.automaticActions("infile", "correctingActions.csv", 2);
////                        graph.TESTremoveNodes();
//                } catch (Exception ex) {
//                    Logger.getLogger(GraphInternalFrame.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                        graph.TESTremoveNodes();
                    };
                });
                
                //Test Actions For Metrics
//                vcorrect_button.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        graph.verticalityCorrection();
//                    };
//                });
//                
//                dcorrect_button.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        double a = Double.parseDouble(a_field.getText());
//                        double b = Double.parseDouble(b_field.getText());
//                        graph.densityCorrection2(a,b);
//                    };
//                });
//                
//                scaleC_button.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        double a = Double.parseDouble(a_field.getText());
//                        double b = Double.parseDouble(b_field.getText());
//                        graph.scaleCorrection(a, b);
//                    };
//                });
//                
//                vert_b.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        
//                        vert_field.setText(graph.getVerticality());
//                        nodes_field.setText(graph.getN());
//                        edges_field.setText(graph.getE());
//                    };
//                });
//                nd_b.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        
//                        nd_field.setText(graph.getNDensity());
//                        nodes_field.setText(graph.getN());
//                        edges_field.setText(graph.getE());
//                    };
//                });
//                ed_b.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        
//                        ed_field.setText(graph.getEDensity());
//                        nodes_field.setText(graph.getN());
//                        edges_field.setText(graph.getE());
//                    };
//                });
                
    }
    
    /**
     * Create the graph for this frame
     * Populate RDFModel and the update frame's layout
     * @param layout layout that will be used initially in nodes position in this frame
     */
    public void createGraph(String layout, String params) {
//		graph =  new Graph();
        boolean graphPopulated = false;
        GraphBackendMode configuredMode = GraphBackendMode.resolveDefault();
        boolean hasSelectedPlainDocuments = hasSelectedPlainDocuments();

        //Populate graph with nodes from RDFModel and visualize them
//                switch(getParentProj().getProjectType()){
//                    case RDF_XML:
//                    case RDF_TRIG:
//                    case SWKM:
//                    case WEB:
        if (configuredMode == GraphBackendMode.JENA && !hasSelectedPlainDocuments) {
            // In JENA mode, avoid touching legacy SWKM model unless explicitly needed.
            if (populateGraphUsingCanonicalSnapshot()) {
                Logger.getLogger(GraphInternalFrame.class.getName()).log(
                        Level.INFO,
                        "[VISUALIZATION JENA] Built graph directly from canonical snapshot."
                );
                graphPopulated = true;
            } else if (shouldAllowSwkmVisualizationFallback() && populateGraphUsingCompatibilitySwkmModel()) {
                Logger.getLogger(GraphInternalFrame.class.getName()).log(
                        Level.INFO,
                        "[VISUALIZATION FALLBACK] Used SWKM compatibility model for graph rendering."
                );
                graphPopulated = true;
            }
        } else {
            try {
                graphController.populateGraph(getParentProj().getModel());
                graphPopulated = true;
            } catch (Exception e) {
                // In non-JENA modes, keep compatibility fallback behavior.
                if (populateGraphUsingCanonicalSnapshot()) {
                    Logger.getLogger(GraphInternalFrame.class.getName()).log(
                            Level.INFO,
                            "[VISUALIZATION JENA] Built graph directly from canonical snapshot."
                    );
                    graphPopulated = true;
                } else if (shouldAllowSwkmVisualizationFallback() && populateGraphUsingCompatibilitySwkmModel()) {
                    Logger.getLogger(GraphInternalFrame.class.getName()).log(
                            Level.INFO,
                            "[VISUALIZATION FALLBACK] Used SWKM compatibility model for graph rendering."
                    );
                    graphPopulated = true;
                } else {
                    Logger.getLogger(GraphInternalFrame.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
        if (populateGraphFromPlainDocuments()) {
            graphPopulated = true;
        }
        if (!graphPopulated && !graph.getNodeList().isEmpty()) {
            graphPopulated = true;
        }
        if (!graphPopulated) {
            Logger.getLogger(GraphInternalFrame.class.getName()).log(
                    Level.WARNING,
                    "[VISUALIZATION] Graph population produced no drawable nodes."
            );
        }
//                        break;
//                    case TXT:
//                    case XML:
        if (graphPopulated && !hasSelectedPlainDocuments) {
            runLegacyParityCheck("GraphInternalFrame.createGraph");
        }
//                    default:
//                        assert(false);
//                    break;
//                }
        //Create the ranker of the graph for top-k viewing
        if (graphPopulated) {
            graph.createGraphRanker();
        }
        graph.visualizeGraphElements();

        if (graphPopulated) {
            updateGraph(layout, params);
        }

        //Create a new JScrollpane to accommodate visual graph
        spane = new JScrollPane(graph.getVisualGraph());
        spane.setMinimumSize(new Dimension(260, 500));
        spane.setVisible(true);
        //Add newly created scrollpane to InternalFrame
        add(spane, BorderLayout.CENTER);


        //Create and add mouse listener to enable right-click popup menu
        MouseListener popupListener = new PopupListener(popup);
        graph.getVisualGraph().addMouseListener(popupListener);
    }//end createGraph

    private void runLegacyParityCheck(String operation) {
        GraphBackendMode mode = GraphBackendMode.resolveDefault();
        if (mode != GraphBackendMode.DUAL) {
            return;
        }

        try {
            SwkmCanonicalAdapter swkmAdapter = new SwkmCanonicalAdapter();
            JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
            ParityComparator comparator = new ParityComparator();
            RDFModel parityModel = resolveSwkmModelForParity();

            CanonicalGraphSnapshot swkmGraphSnapshot = swkmAdapter.fromGraph(graph);
            CanonicalRdfSnapshot swkmRdfSnapshot = swkmAdapter.fromRdfModel(parityModel);

            CanonicalRdfSnapshot jenaRdfSnapshot = jenaAdapter.fromRdfDocuments(getParentProj().getModelDocuments());
            CanonicalGraphSnapshot jenaGraphSnapshot = jenaAdapter.toGraphSnapshot(jenaRdfSnapshot);

            ParityReport rdfReport = comparator.compareRdf(swkmRdfSnapshot, jenaRdfSnapshot);
            logParityReport(operation + " [rdf]", rdfReport);

            ParityReport graphReport = comparator.compareGraph(swkmGraphSnapshot, jenaGraphSnapshot);
            logParityReport(operation + " [graph]", graphReport);
            logLegacyBackendStatus(operation, mode, true, rdfReport, graphReport);
        } catch (Exception ex) {
            logLegacyBackendStatus(operation, mode, false, null, null);
            Logger.getLogger(GraphInternalFrame.class.getName()).log(
                    Level.WARNING,
                    "[PARITY ERROR] " + operation + " failed",
                    ex
            );
        }
    }

    private boolean shouldAllowSwkmVisualizationFallback() {
        return GraphBackendMode.resolveDefault() == GraphBackendMode.DUAL;
    }

    private boolean hasSelectedPlainDocuments() {
        for (String ns : graph.getNamespaces()) {
            if (getParentProj().getPlainDocAsStream(ns) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean populateGraphFromPlainDocuments() {
        boolean populated = false;
        try {
            for (String ns : graph.getNamespaces()) {
                InputStream is = getParentProj().getPlainDocAsStream(ns);
                if (is != null) {
                    graphController.populateGraph(is, ns);
                    populated = true;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphInternalFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RuntimeException ex) {
            Logger.getLogger(GraphInternalFrame.class.getName()).log(Level.SEVERE,
                    "[VISUALIZATION TXT] Failed to populate graph from plain documents.", ex);
        }
        return populated;
    }

    private void logParityReport(String operation, ParityReport report) {
        if (report == null) {
            return;
        }
        if (report.isEquivalent()) {
            System.out.println("[PARITY OK] " + operation + " -> " + report.getDomain());
            return;
        }

        System.out.println("[PARITY FAIL] " + operation + " -> " + report.getDomain()
                + " mismatches=" + report.getMismatches().size());
        for (String mismatch : report.getMismatches()) {
            System.out.println("[PARITY DIFF] " + mismatch);
        }
    }

    private void logLegacyBackendStatus(
            String operation,
            GraphBackendMode configuredMode,
            boolean jenaParsable,
            ParityReport rdfReport,
            ParityReport graphReport
    ) {
        String parityStatus = "N/A";
        if (rdfReport != null && graphReport != null) {
            if (rdfReport.isEquivalent() && graphReport.isEquivalent()) {
                parityStatus = "OK";
            } else {
                int mismatches = rdfReport.getMismatches().size() + graphReport.getMismatches().size();
                parityStatus = "FAIL(" + mismatches + ")";
            }
        }

        String effectiveBackend = jenaParsable ? "JENA" : "SWKM_FALLBACK";
        System.out.println("[BACKEND STATUS] op=" + operation
                + " configured=" + configuredMode
                + " effective=" + effectiveBackend
                + " jena_parsable=" + jenaParsable
                + " parity=" + parityStatus);
    }
	
    private boolean populateGraphUsingCompatibilitySwkmModel() {
        try {
            RDFModel compatibilityModel = buildCompatibilitySwkmModel();
            graphController.populateGraph(compatibilityModel);
            return true;
        } catch (Exception ex) {
            Logger.getLogger(GraphInternalFrame.class.getName()).log(Level.WARNING,
                    "[VISUALIZATION FALLBACK] SWKM compatibility model failed.", ex);
            return false;
        }
    }

    private boolean populateGraphUsingCanonicalSnapshot() {
        try {
            JenaCanonicalAdapter jenaAdapter = new JenaCanonicalAdapter();
            CanonicalRdfSnapshot rdfSnapshot = jenaAdapter.fromRdfDocuments(getParentProj().getModelDocuments());
            CanonicalGraphSnapshot graphSnapshot = jenaAdapter.toGraphSnapshot(rdfSnapshot);
            graphController.populateGraph(graphSnapshot);
            return true;
        } catch (Exception ex) {
            Logger.getLogger(GraphInternalFrame.class.getName()).log(Level.WARNING,
                    "[VISUALIZATION JENA] Canonical graph population failed.", ex);
            return false;
        }
    }

    private RDFModel buildCompatibilitySwkmModel() {
        RDFModel compatibilityModel = new RDFModel();
        Collection<RdfDocument> docs = getParentProj().getModelDocuments();
        for (RdfDocument doc : docs) {
            if (doc == null || doc.getContent() == null) {
                continue;
            }
            byte[] bytes = doc.getContent().getBytes(StandardCharsets.UTF_8);
            String format = normalizeRdfFormat(doc);
            boolean ok = compatibilityModel.read(
                    new ByteArrayInputStream(bytes),
                    doc.getURI(),
                    format,
                    true
            );
            if (!ok) {
                throw new IllegalStateException("Failed to load compatibility SWKM model for " + doc.getURI());
            }
        }
        return compatibilityModel;
    }

    private RDFModel resolveSwkmModelForParity() {
        RDFModel projectModel = getParentProj().getModel();
        try {
            projectModel.getAllInstances();
            return projectModel;
        } catch (Exception ignored) {
            return buildCompatibilitySwkmModel();
        }
    }

    private String normalizeRdfFormat(RdfDocument document) {
        if (document == null || document.getFormat() == null) {
            return "RDF/XML";
        }
        String format = String.valueOf(document.getFormat()).trim();
        if ("RDF_XML".equalsIgnoreCase(format) || "RDFXML".equalsIgnoreCase(format)) {
            return "RDF/XML";
        }
        return format;
    }

	/**
	 * Restore graph for this visualization frame using saved data from specified file
	 * @param fileName name of the file that contains the saved data needed to restore this frame
	 */
	public void restoreGraph(String fileName, boolean createIsa){
		//graph =  new Graph();
                //String[] ns = parent.getModel().getNamespaces();
                
                
                //graph.setGraphNameSpaces(ns);
		//Populate graph with nodes from RDFModel and visualize them
		//graph.populateGraph(parent.getModel()/*, parent.getNamespaceUri()*/,createIsa);
		//graph.visualizeGraphElements();

		graphController.restoreGraph(getParentProj().getModel(),fileName);
		
		//Create a new JScrollpane to accommodate visual graph
		spane = new JScrollPane(graph.getVisualGraph());
		spane.setMinimumSize(new Dimension(260,500));
        spane.setVisible(true);
        //Add newly created scrollpane to InternalFrame
        add(spane,BorderLayout.CENTER);
        //Create and add mouse listener to enable right-click popup menu
            MouseListener popupListener = new PopupListener(popup);
            graph.getVisualGraph().addMouseListener(popupListener);
	}//end restoreGraph
	
	/**
	 * Update graph's layout according user's choice
	 * @param layout new graph layout
	 * @param params all needed params for this layout
	 */
	public boolean updateGraph(String layout, String params){
                Point cOfWindow = null;
                    int width = this.getWidth();
                    int heigth = this.getHeight();
                    cOfWindow = new Point(width/2,heigth/2);
		return graphController.updateLayout(layout, params,cOfWindow);
	}//end updateGraph
	
	/**
	 * Return graph reference of this InternalFrame
	 * @return graph
	 */
	public Graph getGraph(){
		return graph;
	}//end getGraph
	
        /**
         * Returns the number of the selected nodes
         * @return the number of the selected nodes
         */
        private int getGraphSelectedNodes(){
            return graph.getVisualGraph().getSelectedNodesCount();
        }
        
        /**
         * Given a path and a description of an icon it creates an ImageIcon
         * @param path - the path that the icon is located
         * @param description - the description of the icon
         * @return the icon as an ImageIcon
         * 
         */
        private ImageIcon createImageIcon(String path,
                                           String description) {
                java.net.URL imgURL = getClass().getResource(path);
                if (imgURL != null) {
                      return new ImageIcon(imgURL, description);
                } else {
                       System.err.println("Couldn't find file: " + path);
                      return null;
                }
        }
        
	/**
	 * Rank InternalFrame's graph by requested method using provided parameters  
	 * @param method string representation of requested method
	 * @param params parameters required be rank method
	 * @return true upon successful ranking, false otherwise
	 */
	public boolean rankGraph(String method, String params){
//		if(graph.rank(method,params,getParentProj().getNamespaceUri()) != null)
                if(graphController.rank(method,params,getParentProj().getName()) != null)
			return true;
		else return false;
	}//end updateGraph
	
	/**
	 * Rank InternalFrame's graph by requested method using provided parameters and store results in provided file name
	 * @param fileName file's name that will store rank results
	 * @param params parameters required be rank method
	 * @param methods string representation of requested method
	 * @return true upon successful file generation, false otherwise
	 */
	public boolean generateScoreFile(String fileName, String params, String methods){	
//		return(graph.getGraphRanker().generateScoreFile(fileName, params,methods,getParentProj().getName(), getParentProj().getNamespaceUri()));
            return(graph.getGraphRanker().generateScoreFile(fileName, params,methods,getParentProj().getName(), getParentProj().getName()));
	}//end generateScoreFile
        
}


/**
 * Popup Listener class that extends MouseAdapter and enable right-click popup menu
 */
class PopupListener extends MouseAdapter{
	
    JPopupMenu popup;
    

    /**
     * Create a new PopupListener that open provided popup menu
     * @param popupMenu popup menu
     */
    public PopupListener(JPopupMenu popupMenu) {
    	popup = popupMenu;
        
    }//end PopupListener
    
    /**
     * MouseAdapter Interface method. When Mouse Pressed call maybeShowPopup in order to decide whether to open or not a popup menu
     */
    public void mousePressed(MouseEvent e) {

        enableDisablePopupSubMenus();
        maybeShowPopup(e);
    }//end mousePressed

    /**
     * MouseAdapter Interface method. When Mouse Released call maybeShowPopup in order to decide whether to open or not a popup menu
     */
    public void mouseReleased(MouseEvent e) {
        Graph tmpG = ((GraphInternalFrame)ProjectManager.getSingleton().getActiveProject().getActiveFrame()).getGraph();
        if(tmpG.isStarGraphActivated()){
           
            if(e.getClickCount() == 2 && e.getModifiers() == InputEvent.BUTTON1_MASK ){
                if(tmpG.getVisualGraph().getSelectedNodesCount() == 1){
                Point cOfWindow = null;
                    int width = ProjectManager.getSingleton().getActiveProject().getActiveFrame().getContentPane().getWidth();
                    int heigth = ProjectManager.getSingleton().getActiveProject().getActiveFrame().getContentPane().getHeight();
                    cOfWindow = new Point(width/2,heigth/2);
                   
                tmpG.executeStarView();
                String params = "1,150,500000,50,100,0,5,5,10";
                
                tmpG.forceDirectedPlacement(params,true);

                System.out.println(heigth  +" "+ width);
                }
            }
        }
        maybeShowPopup(e);
    }//end mouseReleased

    /**
     * Open a new popup menu
     * @param e mouse event
     */
    private void maybeShowPopup(MouseEvent e) {
        if(e.isPopupTrigger()){
            
            popup.show(e.getComponent(),
                       e.getX(), e.getY()
                       );
            
            
            
        }
    }//end maybeShowPopup
    
    /**
     * Enables or disables some of the menus in the right click depending on the number of selected nodes
     * 
     */
    private void enableDisablePopupSubMenus(){
        int nodesCnt = ((GraphInternalFrame)ProjectManager.getSingleton().getActiveProject().getActiveFrame()).getGraph().getVisualGraph().getSelectedNodesCount();
        int popUpComponentsNum = popup.getComponentCount();
        
        
        //If no node is selected disable the first choices until the separator
        if(nodesCnt == 0){
            
            for(int i = 0;i < popUpComponentsNum;i++){
                if(popup.getComponent(i) instanceof JPopupMenu.Separator){
                    break;
                }
              
                popup.getComponent(i).setEnabled(false);
            }
        }else{
            for(int i = 0;i < popUpComponentsNum;i++){
               popup.getComponent(i).setEnabled(true);
            }
        }
    }
    
}//end class PopupListener