/*
 * GraphModel.java
 *
 * Created on December 6, 2007, 8:19 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author Steve
 */
public class GraphModel {
    
    private LinkedList graphModel;
    
    /** Creates a new instance of GraphModel */
    public GraphModel() {
        graphModel = new LinkedList();
    }
    
    public void addNode(int x, int y) {
        //search list for node with this x and y
        //if it doesn't exist, create the node
        boolean inList = false;
        GraphSpace node;
        
        for(int i = 0; i < graphModel.size(); i++) {
            node = (GraphSpace)graphModel.get(i);
            if(node.getX() == x && node.getY() == y) {
                inList = true;
            }
        }
        
        if(!inList) {
            graphModel.add(new GraphSpace(x, y));
        }
        
    }
    
    public boolean visitNode(int x, int y) {
        GraphSpace node;
        boolean previouslyVisited = false;
        
        for(int i = 0; i < graphModel.size(); i++) {
            node = (GraphSpace)graphModel.get(i);
            if(node.getX() == x && node.getY() == y) {
                previouslyVisited = node.isVisited();
                node.setVisited(true);
                i = graphModel.size();
            }
        }
        return previouslyVisited;
    }
    
    public void clearVisited() {
        GraphSpace node;
        
        for(int i = 0; i < graphModel.size(); i++) {
            node = (GraphSpace)graphModel.get(i);
            node.setVisited(false);
        }
    }
    
    public GraphSpace getNode(int x, int y) {
        GraphSpace node;
        
        for(int i = 0; i < graphModel.size(); i++) {
            node = (GraphSpace)graphModel.get(i);
            if(node.getX() == x && node.getY() == y) {
                return node;
            }
        }
        return null;
    }
    
    public Iterator<GraphSpace> iterator(int index) {
        return graphModel.listIterator(index);
    }
}
