/*
 * Blackboard.java
 *
 * Created on December 6, 2007, 4:52 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author Ben Chavet
 */
public class Blackboard {

    private LinkedList blackboard;
    
    /** Creates a new instance of Blackboard */
    public Blackboard() {
        this.blackboard = new LinkedList();
    }
    
    /**
     * Post a BlackboarMessage to the blackboard
     */
    public boolean post(BlackboardMessage message) {
        //System.out.println("Blackboard Message Posted");
        return this.blackboard.add(message);
    }
    
    /**
     * Read the first message on the blackboard
     */
    public BlackboardMessage readFirst() {
        //System.out.println("Blackboard Message Read");
        return (BlackboardMessage) this.blackboard.getFirst();
    }
    
    public int getSize() {
        return this.blackboard.size();
    }
    
    /**
     * Respond to the given blackboard message, which removes it from the
     * blackboard.
     */
    public boolean respond(BlackboardMessage message) {
        //System.out.println("Blackboard Message Respoded to");
        return this.blackboard.remove(message);
    }

    /**
     * Get an iterator for the blackboard messages, allowing an agent to do
     * whatever type of search it wishes.
     */
    public Iterator<BlackboardMessage> iterator(int index) {
        return this.blackboard.listIterator(index);
    }
    
}
