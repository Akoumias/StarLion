/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package graphs;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 *
 * @author zabetak
 */
public class InstanceCache {
    
    private Graph   g;
    private VisualGraph visG;
    private Hashtable<String,InstanceRecord> data;
    private HashMap<String,Integer> contents;

    //Newest element is in the first position
    private LinkedList<String>   dataOrder;
    private int cacheSize = 20;//Default

    public InstanceCache(Graph graph,int size){
        g = graph;
        visG= g.getVisualGraph();
        if(size > 0){
            cacheSize = size;
        }
        data = new Hashtable<String, InstanceRecord>();
        contents = new HashMap<String,Integer>();
        dataOrder = new LinkedList<String>();
    }

    public InstanceCache() {

    }

//    public InstanceCache(VisualGraph visualGraph,int size){
//        visG = visualGraph;
//        if(size > 0){
//            cacheSize = size;
//        }
//        data = new Hashtable<String, InstanceRecord>();
//        dataOrder = new LinkedList<String>();
//    }

    public void add(InstanceRecord ir){
        if(data.contains(ir.getName())){
            dataOrder.remove(ir.getName());
            dataOrder.addFirst(ir.getName());
        }else{
            if(isFull()){
                replacementPolicy(ir);
            }else{
                data.put(ir.getName(),ir);
                dataOrder.addFirst(ir.getName());
                
                addInstanceNodesInGraph(ir);
            }
            visG.visualizeGraph();
            visG.updateGraph();
        }

    }

    private void addInstanceNodesInGraph(InstanceRecord ir){
        for(Node n:ir.getNodes()){
                    
                    Integer ref = contents.get(n.getName());
                    if(ref == null){
                        g.getNodeList().put(n.getName(), n);
                        visG.addNode(n);
                        contents.put(n.getName(),0);
//                        System.out.println("AddedNode:"+n);
                    }else{

                        ref = 1 ;
//                        System.out.println("IncreaseRefNode:"+n+":ref:"+ref);
                        contents.put(n.getName(),ref);
                    }
                }
                for(Edge e:ir.getEdges()){
                    String edgeID = e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName();
                    Integer ref = contents.get(edgeID);
                    if(ref == null){
                        g.getEdgeList().put(edgeID,e);
                        visG.addEdge(e);
                        contents.put(edgeID,0);
//                        System.out.println("AddedEdge:"+e);
                    }else{

                        ref = 1;
//                        System.out.println("IncreaseRefEdge:"+e+":ref:"+ref);
                        contents.put(edgeID,ref);
                    }
                }
//                System.out.println("GraphCells = "+visG.getGraphCells().size());
//                visG.visualizeGraph();
//                visG.getGraphLayoutCache().insert(ir.getCells().toArray());
//                visG.updateGraph();
    }
    
    private void updateInstanceNodesInGraph(InstanceRecord ir){
        
//        for(Node n:ir.getNodes()){
//            if(!g.getNodeList().containsKey(n.getName())){
//                g.getNodeList().put(n.getName(), n);
////                Node newN = new Node(n);
////                System.out.println("OLDN hash:"+n.hashCode()+" NEWN hash"+newN.hashCode());
//                visG.addNode(n);
//                System.out.println("UpdatedNODE:"+n);
//            }
//        }
        for(Edge e:ir.getEdges()){
            if(!g.getEdgeList().containsKey(e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName())){
//                g.getEdgeList().put(e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName(),e);
//                Edge newE = new Edge(e);
//                visG.addEdge(e);
                String srcName = e.getSourceNode().getName();
                String dstName = e.getTargetNode().getName();
                Node srcNode = ir.getNode(srcName);
//                Node srcNode = g.getNodeList().get(srcName);
//                if(srcNode == null){
//                    
//                    srcNode = new Node(g, null, srcName, 0, 0, 0, 30, 30, srcName, SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
//                    g.getNodeList().put(srcNode.getName(), srcNode);
//                    visG.addNode(srcNode);
////                Node newN = new Node(n);
////                System.out.println("OLDN hash:"+n.hashCode()+" NEWN hash"+newN.hashCode());
//                
////                System.out.println("UpdatedNODE:"+n);
//            }
             Node dstNode = ir.getNode(dstName);
//                Node dstNode = g.getNodeList().get(dstName);
//                if(dstNode == null){
//                    
//                    dstNode = new Node(g, null, dstName, 0, 0, 0, 30, 30, dstName, SEMWEB_OBJECT_TYPE.CLASSINSTANCE);
//                    g.getNodeList().put(dstNode.getName(), dstNode);
//                    visG.addNode(dstNode);
////                Node newN = new Node(n);
////                System.out.println("OLDN hash:"+n.hashCode()+" NEWN hash"+newN.hashCode());
//                
////                System.out.println("UpdatedNODE:"+n);
//            }
//             System.out.println("DSTNODE:"+dstNode.getName()+":G:"+dstNode.getGraphNode().getChildAt(0));
                Edge ne = new Edge(g, srcNode, dstNode, e.toString(), e.rdfObjectType, 0, true);
                g.getEdgeList().put(ne.toString()+ne.getSourceNode()+ne.getTargetNode(), ne);
//                System.out.println("UpdatedEdge:"+ne);
                visG.addEdge(ne);
            }
        }
        
    }
    
    private void removeInstanceNodesFromGraph(InstanceRecord ir){
//        System.out.println("CELL SIZE BEFORE:"+visG.getGraphLayoutCache().getCellViews().length);
//        System.out.println("NodeList size="+g.getNodeList().size());
//        Object[] nodeCells = new Object[ir.getNodes().size()];
//        int i = 0;
//        for(Node n:ir.getNodes()){
//            if(!data.containsKey(n.getName())){
//                    g.getNodeList().remove(n.getName());
//                    System.out.println("RemovedNODE:"+n);
//                    visG.removeNode(n);
//            }
////                    nodeCells[i++]= n.getGraphNode();
//        }
//        System.out.println("NodeList size="+g.getNodeList().size());
//                System.out.println("EdgeList size="+g.getEdgeList().size());
//                for(Edge e:ir.getEdges()){
//                    g.getEdgeList().remove(e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName());
//                    visG.removeEdge(e);
//                    System.out.println("RemovedEdge:"+e);
//                }

        for(Node n:ir.getNodes()){
                    Integer ref = contents.get(n.getName());
                    if(ref == 0){
                        g.getNodeList().remove(n.getName());
                        contents.remove(n.getName());
//                        System.out.println("RemovedNODE:"+n);
                        visG.removeNode(n);
                    }else if(ref > 0){
                        contents.put(n.getName(), ref--);
                    }
                }
        for(Edge e:ir.getEdges()){
            String edgeID = e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName();
            Integer ref = contents.get(edgeID);
            if(ref == 0){

                g.getEdgeList().remove(edgeID);
                contents.remove(edgeID);
//                System.out.println("RemovedEdge:"+e);
                visG.removeEdge(e);
            }else if(ref > 0){
                contents.put(edgeID, ref--);
            }
        }
//                for(Edge e:ir.getEdges()){
//                    Node srcN = e.getSourceNode();
//                    Node dstN = e.getTargetNode();
//                    boolean shouldRemoveEdge = true;
//                    Integer ref = contents.get(srcN.getName());
//                    if(ref == 1){
//                        g.getNodeList().remove(srcN.getName());
//                        contents.remove(srcN.getName());
//                        System.out.println("RemovedNODE:"+srcN);
//                        visG.removeNode(srcN);
//                        shouldRemoveEdge = false;
//                    }else if(ref > 1){
//                        contents.put(srcN.getName(), ref--);
//                    }
//                    ref = contents.get(dstN.getName());
//                    if(ref == 1){
//                        g.getNodeList().remove(dstN.getName());
//                        contents.remove(dstN.getName());
//                        System.out.println("RemovedNODE:"+dstN);
//                        visG.removeNode(dstN);
//                        shouldRemoveEdge = false;
//                    }else if(ref > 1){
//                        contents.put(dstN.getName(), ref--);
//                    }
////                    if(!data.containsKey()){
////                        g.getNodeList().remove(srcN.getName());
////                        contents.remove(srcN.getName());
////                        System.out.println("RemovedNODE:"+srcN);
////                        visG.removeNode(srcN);
////                        shouldRemoveEdge = false;
////                    }
////                    if(!data.containsKey(contents.get(dstN.getName()))){
////                        g.getNodeList().remove(dstN.getName());
////                        contents.remove(dstN.getName());
////                        System.out.println("RemovedNODE:"+dstN);
////                        visG.removeNode(dstN);
////                        shouldRemoveEdge = false;
////                    }
//                    String edgeID = e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName();
//                    ref = contents.get(srcN.getName());
//                    if(ref == 1){
//                        g.getNodeList().remove(srcN.getName());
//                        contents.remove(srcN.getName());
//                        System.out.println("RemovedNODE:"+srcN);
//                        visG.removeNode(srcN);
//                        shouldRemoveEdge = false;
//                    }else if(ref > 1){
//                        contents.put(srcN.getName(), ref--);
//                    }
////                    if(shouldRemoveEdge){
////                        g.getEdgeList().remove(edgeID);
////                        contents.remove(e.toString()+e.getSourceNode().getName()+e.getTargetNode().getName());
////                        visG.removeEdge(e);
////                        System.out.println("RemovedEdge:"+e);
////                    }
//                }
                
                
//                System.out.println("EdgeList size="+g.getEdgeList().size());
//                visG.getGraphLayoutCache().removeCells(ir.getCells().toArray());
                
//                visG.getGraphLayoutCache().remove(ir.getCells().toArray(),false,true);

//                visG.visualizeGraph();
//                visG.updateGraph();
//                System.out.println("CELL SIZE AFTER:"+visG.getGraphLayoutCache().getCellViews().length);
    }
    
    private void replacementPolicy(InstanceRecord ir){
        //LRU Replacement Policy
        String elemToRemove = dataOrder.removeLast();
        InstanceRecord elem = data.remove(elemToRemove);
        //Apply the proper remove actions to the visGraph
        removeInstanceNodesFromGraph(elem);
        //visG.getGraphLayoutCache().removeCells(elem.getCells().toArray());
        data.put(ir.getName(),ir);
        dataOrder.addFirst(ir.getName());
        //Apply the proper add actions to the visGraph
        addInstanceNodesInGraph(ir);
        //visG.getGraphLayoutCache().insert(ir.getCells());
        
    }

    public boolean isFull(){
        if(cacheSize == data.size()){
            return true;
        }else{
            return false;
        }
    }

    public InstanceRecord get(String instance){
        if(data.contains(instance)){
//            dataOrder.remove(ir);
//            dataOrder.addFirst(ir);
            return data.get(instance);
        }else{
            return null;
        }
    }

    public boolean contains(String instance){
        if(data.containsKey(instance)){
            dataOrder.remove(instance);
            dataOrder.addFirst(instance);
//            InstanceRecord ir = data.get(instance);
//            updateInstanceNodesInGraph(ir);
//            visG.visualizeGraph();
//            visG.updateGraph();
            return true;
        }else{
            return false;
        }
//            //Also make an update to ensure that neighbour nodes are loaded
//            InstanceRecord ir = data.get(instance);
//            updateInstanceNodesInGraph(ir);
//            return true;
//        }else{
//            return false;
//        }
//        return data.containsKey(instance);
    }

    public void CacheContents(){
        int cnt = 0;
        for(InstanceRecord ir:data.values()){
            System.out.println();
            System.out.println("INSTANCE NO. "+cnt+" "+ir.getName());
            System.out.println("----------------------------------");
            System.out.println("Nodes:");
            for(Node n:ir.getNodes()){
                System.out.println(n.getName());
            }
            System.out.println("Edges:");
            for(Edge e:ir.getEdges()){
                System.out.println(e);
            }
            System.out.println();
            cnt++;
        }
    }
    
    public void nodeListContents(){
        System.out.println("**Node List Contents**");
        for(Node n:g.getNodeList().values()){
            System.out.println(n.getName());
        }
    }
    
    /**
     * 
     * @return the number of instances that are contained in the cache
     */
    public int getRealSize(){
        return data.values().size();
    }

    /**
     * 
     * @return the maximun number of instances that the cache can hold without 
     * applying replacement policy
     */
    public int getMaxSize(){
        return cacheSize;
    }
    
    /**
     * 
     * @return the graph in which the cache is associated
     */
    public Graph getAssociateGraph(){
        return g;
    }
    
    /**
     * Clear the cache
     */
    public void clear(){
        for(InstanceRecord ir:data.values()){
            removeInstanceNodesFromGraph(ir);
        }
        data.clear();
        dataOrder.clear();
        contents.clear();
    }
}
