/*
 * VisitedSpace.java
 *
 * Created on December 6, 2007, 8:15 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

/**
 *
 * @author Steve
 */
public class GraphSpace {
    
    private int x;
    private int y;
    private boolean visited;
    
    /** Creates a new instance of VisitedSpace */
    public GraphSpace(int x, int y) {
        this.x = x;
        this.y = y;
        setVisited(false);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
    
}
