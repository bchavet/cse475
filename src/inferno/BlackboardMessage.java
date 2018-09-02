/*
 * BlackboardMessage.java
 *
 * Created on December 6, 2007, 5:11 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

/**
 *
 * @author Ben Chavet
 */
public class BlackboardMessage {

    /** X coordinate for the message */
    private int x;
    
    /** Y coordinate for the message */
    private int y;
    
    /** Direction of fire from message coordinage (N, S, E, W) */
    private int direction;
    
    /** Strength of fire at the location described by the message */
    private int fireStrength;
    
    /**
     * Creates a new instance of BlackboardMessage
     */
    public BlackboardMessage(int x, int y, int direction, int fireStrength) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fireStrength = fireStrength;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getFireStrength() {
        return fireStrength;
    }

    public void setFireStrength(int fireStrength) {
        this.fireStrength = fireStrength;
    }

}
