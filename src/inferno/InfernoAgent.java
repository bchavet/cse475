/*
 * InfernoAgent.java
 *
 * Created on November 11, 2007, 6:32 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

import inferno.*;
import java.awt.Color;
import javax.xml.soap.SAAJMetaFactory;
import java.util.*;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

/**
 *
 * @author Steve Mott
 * @author Ben Chavet
 */
public class InfernoAgent implements Drawable {
    
    private int x;
    private int y;
    private int energy;
    private int visibility;
    
    //The following two variables can be used to change the shot strength of the agents
    //and the extra boost of energy the agents recieve for fully extiguishing a fire.
    private int shotStrengthModifier;
    private int energyGainModifier;
    
    private boolean taskToDo;
    
    private Stack mazeNav;
    
    private GraphModel map;
    
    private BlackboardMessage task;
    
    private Blackboard blackboard;
    private InfernoSpace space;
    private Random random;
    
    
    /** Creates a new instance of InfernoAgent */
    public InfernoAgent(InfernoSpace ss, Blackboard bb, Random rr) {
        visibility = 5;
        space = ss;
        blackboard = bb;
        taskToDo = false;
        mazeNav = new Stack();
        map = new GraphModel();
        setEnergy(1000);
        
        //Set the random function to the one used by InfernoModel
        random = rr;
        
        map.addNode(getX(), getY());
    }
    
    /**
     * all reasoning and actions for the firefighter agent each step will take place here
     */
    public void step() {
        this.dealDamage();
        // Check if current space is on fire
        if (this.space.getFireStrength(this.getX(), this.getY()) > 0) {
            // This space is on fire, obviously whatever we were doing wasn't
            // working if the fire spread to this space, better call for help
            blackboard.post(new BlackboardMessage(this.getX(), this.getY(), 0, space.getFireStrength(this.getX(), this.getY())));
            
            // Then, try to find a safe space to go to, look for the space with the lowest flamability
            int lowestFlamability = this.space.getFlamability(this.getX(), this.getY());
            int direction = 0;
            
            // Check NORTH
            if (this.canMoveSafely(InfernoSpace.NORTH)) {
                lowestFlamability = this.space.getFlamability(this.getX(InfernoSpace.NORTH), this.getY(InfernoSpace.NORTH));
                direction = InfernoSpace.NORTH;
            }
            
            // Check SOUTH
            if (this.canMoveSafely(InfernoSpace.SOUTH)) {
                lowestFlamability = this.space.getFlamability(this.getX(InfernoSpace.SOUTH), this.getY(InfernoSpace.SOUTH));
                direction = InfernoSpace.SOUTH;
            }
            
            // Check EAST
            if (this.canMoveSafely(InfernoSpace.EAST)) {
                lowestFlamability = this.space.getFlamability(this.getX(InfernoSpace.EAST), this.getY(InfernoSpace.EAST));
                direction = InfernoSpace.EAST;
            }
            
            // Check WEST
            if (this.canMoveSafely(InfernoSpace.WEST)) {
                lowestFlamability = this.space.getFlamability(this.getX(InfernoSpace.WEST), this.getY(InfernoSpace.WEST));
                direction = InfernoSpace.WEST;
            }
            
            // Check if we found a safe escape
            if (direction != 0) {
                // A safe space was found, go there
                this.move(direction);
                
                // Now that we're safe, check for an escape route
                if (this.escapeRoute()) {
                    // If there is one, go about normal decision making
                    this.findJob();
                } else {
                    // Otherwise, try to make an escape route by fighting the
                    // fire you are most likely to be able to put out the quickest
                    this.createEscapeRoute();
                }
                
            } else {
                // No safe spaces were found, figure out where your best odds are
                int estimated;
                int lowestEstimated = this.estimateFireIntensity(this.getX(), this.getY(), this.getEnergy());
                direction = 0;
                
                // Check NORTH
                estimated = this.estimateFireIntensity(this.getX(InfernoSpace.NORTH), this.getY(InfernoSpace.NORTH), this.getEnergy());
                if (estimated < lowestEstimated) {
                    lowestEstimated = estimated;
                    direction = InfernoSpace.NORTH;
                }
                // Check SOUTH
                estimated = this.estimateFireIntensity(this.getX(InfernoSpace.SOUTH), this.getY(InfernoSpace.SOUTH), this.getEnergy());
                if (estimated < lowestEstimated) {
                    lowestEstimated = estimated;
                    direction = InfernoSpace.SOUTH;
                }
                // Check EAST
                estimated = this.estimateFireIntensity(this.getX(InfernoSpace.EAST), this.getY(InfernoSpace.EAST), this.getEnergy());
                if (estimated < lowestEstimated) {
                    lowestEstimated = estimated;
                    direction = InfernoSpace.EAST;
                }
                // Check WEST
                estimated = this.estimateFireIntensity(this.getX(InfernoSpace.WEST), this.getY(InfernoSpace.WEST), this.getEnergy());
                if (estimated < lowestEstimated) {
                    lowestEstimated = estimated;
                    direction = InfernoSpace.WEST;
                }
                
                this.move(direction);
                this.shoot(this.getX(), this.getY());
                
            }
            
        } else {
            // The current space is not on fire, check for an escape route
            if (this.escapeRoute()) {
                // If there is one, go about normal decision making
                this.findJob();
            } else {
                // Otherwise, we're trapped.  Call for help!
                blackboard.post(new BlackboardMessage(this.getX(), this.getY(), 0, 0));
                
                // Then, try to create an escape route.
                this.createEscapeRoute();
            }
        }
        
    }
    
    public void dealDamage() {
        int fireStrength = space.getFireStrength(this.getX(), this.getY());
        if (fireStrength > 0) {
            this.setEnergy(this.getEnergy() - fireStrength);
        }
    }
    
    public void findJob() {
        //System.out.println("Looking for a job");
        int x = getX();
        int y = getY();
        
        
        // First find the closest fire within your visibility range
        boolean foundFire = false;
        int closestFire = 2 * space.getSize();
        int closestFireX = 0;
        int closestFireY = 0;
        for(int i = visibility * -1; i <= visibility; i++) {
            for(int j = visibility * -1; j <= visibility; j++) {
                if(x + i < space.getSize() && x + i >= 0 && y + j < space.getSize() && y + j >= 0) {
                    if(space.getFireStrength(x + i, y + j) > 0) {
                        foundFire = true;
                        if(Math.abs(i) + Math.abs(j) < closestFire) {
                            closestFire = Math.abs(i) + Math.abs(j);
                            closestFireX = x + i;
                            closestFireY = y + j;
                        }
                    }
                }
            }
        }
        
        // Check if we are already working on a task
        if (this.taskToDo) {
            // If we found a fire, see if it is closer than our task
            if (foundFire) {
                int taskDistance = Math.abs(this.task.getX() - this.getX()) + Math.abs(this.task.getY() - this.getY());
                if (closestFire < taskDistance) {
                    // If so, decide to either continue or abort our task
                    int rand = random.nextInt(4);
                    if (rand == 0) {
                        this.task = new BlackboardMessage(closestFireX, closestFireY, 0, 0);
                        blackboard.post(this.task);
                    }
                }
            }
            
            // Keep moving toward our task
            this.moveToTask();
            
        } else if (foundFire) {
            // We don't have a task, but we found a fire, post it, and head toward it
            this.taskToDo = true;
            this.task = new BlackboardMessage(closestFireX, closestFireY, 0, space.getFireStrength(closestFireX, closestFireY));
            blackboard.post(this.task);
            this.moveToTask();
            
        } else {
            // Check the blackboard for something to do
            if (this.searchBlackboard()) {
                moveToTask();
                
            } else {
                // Nothing to do, randomly wander
                
                // Check if any edges are in visibility range
                int closestEdge = this.visibility;
                int direction = 0;
                if (this.getX() <= this.visibility && this.getX() < closestEdge) {
                    closestEdge = this.getX();
                    direction = InfernoSpace.EAST; // Closest to west edge, move east
                }
                if (this.space.getSize() - this.getX() - 1 <= this.visibility && this.space.getSize() - this.getX() - 1 < closestEdge) {
                    closestEdge = this.space.getSize() - this.getX() - 1;
                    direction = InfernoSpace.WEST; // Closest to east edge, move west
                }
                if (this.getY() <= this.visibility && this.getY() < closestEdge) {
                    closestEdge = this.getY();
                    direction = InfernoSpace.SOUTH; // Closest to north edge, move south
                }
                if (this.space.getSize() - this.getY() - 1 <= this.visibility && this.space.getSize() - this.getY() - 1 < closestEdge) {
                    closestEdge = this.space.getSize() - this.getY() - 1;
                    direction = InfernoSpace.NORTH; // Closest to south edge, move north
                }
                
                // Try to move in the specified direction
                if (!this.move(direction)) {
                    // If that doesn't work, pick a random direction
                    int rand = random.nextInt(4);
                    switch (rand) {
                        case 0:
                            this.move(InfernoSpace.NORTH);
                            break;
                        case 1:
                            this.move(InfernoSpace.SOUTH);
                            break;
                        case 2:
                            this.move(InfernoSpace.EAST);
                            break;
                        case 3:
                            this.move(InfernoSpace.WEST);
                            break;
                    }
                }
                
            }
        }
        
    }
    
    public void moveToTask() {
        //System.out.println("Moving to task");
        GraphSpace node;
        
        int direction = 0;
        int dirX = 0;
        int dirY = 0;
        
        int newPos = 0;
        int distanceFromGoal = space.getSize() * 2;
        int newX = 0;
        int newY = 0;
        
        int currentPosX = getX();
        int currentPosY = getY();
        
        int taskX = task.getX();
        int taskY = task.getY();
        
        int distanceX = taskX - currentPosX;
        int distanceY = taskY - currentPosY;
        int totalDistance = Math.abs(distanceX) + Math.abs(distanceY);
        
            /*
             *This huge if statement is pretty much all of the agent's reasoning for navigating the maze of fire.
             *First, it checks to make sure it isn't already basically looking at it... good idea, huh?
             *
             *Then it figures in which direction it is furthest away from the target and tries to move accordingly
             *if the optimal space is on fire, then the agent will try to find the second optimal space
             *
             *The second optimal space is the one that decreases the agents' distance from the target in the oppposite
             *direction as the first try.
             *
             *If the second optimal space is taken, it finds the space that will keep it closest to the target, but
             *it only considers spaces that aren't on fire and haven't already been visited (implementing a hybrid of
             *a previously learned sorting algorithm)
             *
             *While this method may not be the most effecient way to navigate a maze, I feel that it is the most
             *realistic. A real person is going to try and get as close to their target as fast as possible. They
             *also try as hard as they can to stay as close as possible to their target.
             */
        
        
        if (totalDistance == 1) {
            blackboard.respond(task);
            // Agent is adjacent to the task space
            if (space.getFireStrength(task.getX(), task.getY()) > 0) {
                shoot(task.getX(), task.getY());
                // There's still fire here, re-post message
                if (space.getFireStrength(task.getX(), task.getY()) > 0) {
                    blackboard.post(task);
                }
            } else {
                taskToDo = false;
                map.clearVisited();
            }
            return; // We're done here
        }
        
        // If our goal is in our visibility range, see if there is still fire there
        if (Math.abs(this.getX() - task.getX()) < this.getVisibility() && Math.abs(this.getY() - task.getY()) < this.getVisibility()) {
            // If we can see that there is no more fire, abort
            if (space.getFireStrength(task.getX(), task.getY()) <= 0) {
                taskToDo = false;
                map.clearVisited();
                return;
            }
        }
        
        dirX = (distanceX < 0) ? InfernoSpace.WEST : InfernoSpace.EAST;
        dirY = (distanceY < 0) ? InfernoSpace.NORTH : InfernoSpace.SOUTH;
        
        // Which direction should we try first?
        if (Math.abs(distanceX) > Math.abs(distanceY)) {
            direction = dirX;
        } else {
            direction = dirY;
        }
        if (this.canMoveSafely(direction) && !map.visitNode(this.getX(direction), this.getY(direction))) {
            this.move(direction);
            mazeNav.push(map.getNode(this.getX(), this.getY()));
            
        } else {
            // Our first direction didn't work, figure out which way to go now
            if (direction == dirX) {
                direction = dirY;
            } else {
                direction = dirX;
            }
            if (this.canMoveSafely(direction) && !map.visitNode(this.getX(direction), this.getY(direction))) {
                this.move(direction);
                mazeNav.push(map.getNode(this.getX(), this.getY()));
                
            } else {
                // That direction didn't work either, see if we can go another direction
                if (this.canMoveSafely(InfernoSpace.NORTH) && !map.visitNode(this.getX(InfernoSpace.NORTH), this.getY(InfernoSpace.NORTH))) {
                    this.move(InfernoSpace.NORTH);
                    mazeNav.push(map.getNode(this.getX(), this.getY()));
                } else if (this.canMoveSafely(InfernoSpace.SOUTH) && !map.visitNode(this.getX(InfernoSpace.SOUTH), this.getY(InfernoSpace.SOUTH))) {
                    this.move(InfernoSpace.SOUTH);
                    mazeNav.push(map.getNode(this.getX(), this.getY()));
                } else if (this.canMoveSafely(InfernoSpace.EAST) && !map.visitNode(this.getX(InfernoSpace.EAST), this.getY(InfernoSpace.EAST))) {
                    this.move(InfernoSpace.EAST);
                    mazeNav.push(map.getNode(this.getX(), this.getY()));
                } else if (this.canMoveSafely(InfernoSpace.WEST) && !map.visitNode(this.getX(InfernoSpace.WEST), this.getY(InfernoSpace.WEST))) {
                    this.move(InfernoSpace.WEST);
                    mazeNav.push(map.getNode(this.getX(), this.getY()));
                }
            }
        }
        
    }
    
    //estimate intensities for each spot if shot...
    
    //look for little to no fuel spaces and low flamability spaces
    //if flamability is lower than energy, shoot it!
    //if fuel = 0... shoot it, it's goin down!
    //then look for the best combination of the two
    public boolean escapeRoute() {
        if (this.canMoveSafely(InfernoSpace.NORTH)) {
            return true;
        }
        if (this.canMoveSafely(InfernoSpace.SOUTH)) {
            return true;
        }
        if (this.canMoveSafely(InfernoSpace.EAST)) {
            return true;
        }
        if (this.canMoveSafely(InfernoSpace.WEST)) {
            return true;
        }
        return false;
    }
    
    public void createEscapeRoute() {
        int bestEscapeRouteVal;
        int escapeRouteX, escapeRouteY;
        int nextEscapeRouteVal;
        
        bestEscapeRouteVal = 100;
        escapeRouteX = 0;
        escapeRouteY = 0;
        nextEscapeRouteVal = 100;
        
        if(getX() - 1 >= 0) {
            bestEscapeRouteVal = estimateFireIntensity(getX() - 1, getY(), getEnergy());
            escapeRouteX = getX() - 1;
            escapeRouteY = getY();
        }
        if(getX() + 1 < space.getSize()) {
            nextEscapeRouteVal = estimateFireIntensity(getX() + 1, getY(), getEnergy());
            if(nextEscapeRouteVal < bestEscapeRouteVal) {
                bestEscapeRouteVal = nextEscapeRouteVal;
                escapeRouteX = getX() + 1;
                escapeRouteY = getY();
            }
        }
        if(getY() - 1 >= 0) {
            nextEscapeRouteVal = estimateFireIntensity(getX(), getY() - 1, getEnergy());
            if(nextEscapeRouteVal < bestEscapeRouteVal) {
                bestEscapeRouteVal = nextEscapeRouteVal;
                escapeRouteX = getX();
                escapeRouteY = getY() - 1;
            }
        }
        if(getY() + 1 < space.getSize()) {
            nextEscapeRouteVal = estimateFireIntensity(getX(), getY() + 1, getEnergy());
            if(nextEscapeRouteVal < bestEscapeRouteVal) {
                bestEscapeRouteVal = nextEscapeRouteVal;
                escapeRouteX = getX();
                escapeRouteY = getY() + 1;
            }
        }
        
        shoot(escapeRouteX, escapeRouteY);
        if(space.getFireStrength(escapeRouteX, escapeRouteY) > 0) {
            blackboard.post(new BlackboardMessage(escapeRouteX, escapeRouteY, 'N', space.getFireStrength(escapeRouteX, escapeRouteY)));
        }
    }
    
    public int getShotStrength() {
        return (int)((this.getEnergy() * getShotStrengthModifier() / 100));
    }
    
    public int getEnergyGain(int oldFireStrength, int newFireStrength) {
        int shotStrength = 0;
        int energyGain = 0;
        
        //energyGain = (int)((oldFireStrength - newFireStrength) / 4);
        
        // The fire was put out, give a (linear) bonus
        if(newFireStrength <= 0 && oldFireStrength > 0) {
            energyGain = energyGain + getEnergyGainModifier();
        } else {
            energyGain = 0;
        }
        
        return energyGain;
    }
    
    /**
     * Move agent one space in given direction, accounts for edge detection
     * Returns true on success, false on failure.
     */
    public boolean move(int direction) {
        if (this.canMove(direction)) {
            switch (direction) {
                
                case InfernoSpace.NORTH:
                    this.setY(this.getY() - 1);
                    break;
                    
                case InfernoSpace.SOUTH:
                    this.setY(this.getY() + 1);
                    break;
                    
                case InfernoSpace.EAST:
                    this.setX(this.getX() + 1);
                    break;
                    
                case InfernoSpace.WEST:
                    this.setX(this.getX() - 1);
                    break;
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Determine whether this agent can move in the given direction.  This is
     * simply based on edge detection
     */
    public boolean canMove(int direction) {
        switch (direction) {
            case InfernoSpace.NORTH:
                if (this.getY() > 0) {
                    return true;
                }
                break;
                
            case InfernoSpace.SOUTH:
                if (this.getY() < this.space.getSize() - 1) {
                    return true;
                }
                break;
                
            case InfernoSpace.EAST:
                if (this.getX() < this.space.getSize() - 1) {
                    return true;
                }
                break;
                
            case InfernoSpace.WEST:
                if (this.getX() > 0) {
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    /**
     * Determines whether this agent can SAFELY move in the given direction
     * (i.e., there is no fire there)
     */
    public boolean canMoveSafely(int direction) {
        switch (direction) {
            case InfernoSpace.NORTH:
                return (this.canMove(direction) && this.space.getFireStrength(this.getX(), this.getY() - 1) <= 0);
                
            case InfernoSpace.SOUTH:
                return (this.canMove(direction) && this.space.getFireStrength(this.getX(), this.getY() + 1) <= 0);
                
            case InfernoSpace.EAST:
                return (this.canMove(direction) && this.space.getFireStrength(this.getX() + 1, this.getY()) <= 0);
                
            case InfernoSpace.WEST:
                return (this.canMove(direction) && this.space.getFireStrength(this.getX() - 1, this.getY()) <= 0);
        }
        
        return false;
    }
    
    public void move(int moveToX, int moveToY) {
        //Exception e = new Exception();
        int oldX, oldY;
        oldX = x;
        oldY = y;
        try {
            setX(moveToX);
            setY(moveToY);
            if(Math.abs(moveToX - oldX) + Math.abs(moveToY - oldY) > 1) {
                //throw new Exception();
            }
        } catch (Exception e) {
            e.printStackTrace();
            
        }
        map.addNode(moveToX, moveToY);
    }
    
    public void shoot(int shootX, int shootY) {
        int agentEnergy = 0;
        int newFireStrength = 0;
        int oldFireStrength = 0;
        int agentEnergyGain = 0;
        int newAgentEnergy = 0;
        
        agentEnergy = getEnergy();
        
        oldFireStrength = space.getFireStrength(shootX, shootY);
        newFireStrength = space.fightFire(shootX, shootY, getShotStrength());
        
        agentEnergyGain = getEnergyGain(oldFireStrength, newFireStrength);
        newAgentEnergy = agentEnergy + agentEnergyGain;
        setEnergy(newAgentEnergy);
        
    }
    
    public int getX() {
        return x;
    }
    
    /**
     * Returns the X value in the given direction to the agent.  If the agent
     * is at an edge, and there is no space in the given direction, the agent's
     * current X value is returned.
     */
    public int getX(int direction) {
        if (this.canMove(direction)) {
            switch (direction) {
                case InfernoSpace.EAST: return this.x + 1;
                case InfernoSpace.WEST: return this.x - 1;
            }
        }
        return this.x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    /**
     * Returns the Y value in the given direction to the agent.  If the agent
     * is at an edge, and there is no space in the given direction, the agent's
     * current Y value is returned.
     */
    public int getY(int direction) {
        if (this.canMove(direction)) {
            switch (direction) {
                case InfernoSpace.NORTH: return this.y - 1;
                case InfernoSpace.SOUTH: return this.y + 1;
            }
        }
        return this.y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getEnergy() {
        return energy;
    }
    
    public void setEnergy(int energy) {
        this.energy = energy;
    }
    
    public int getShotStrengthModifier() {
        return shotStrengthModifier;
    }
    
    public void setShotStrengthModifier(int shotStrengthModifier) {
        this.shotStrengthModifier = shotStrengthModifier;
    }
    
    public int getEnergyGainModifier() {
        return energyGainModifier;
    }
    
    public void setEnergyGainModifier(int energyGainModifier) {
        this.energyGainModifier = energyGainModifier;
    }
    
    private int estimateFireIntensity(int x, int y, int energy) {
        int estimatedFireIntensity;
        int intensity;
        int flamability;
        float intensityRate;
        double fuel;
        
        estimatedFireIntensity = 0;
        intensity = 0;
        flamability = 0;
        intensityRate = 0;
        fuel = 0;
        
        intensity = space.getFireStrength(x, y);
        fuel = space.getFuelLevel(x, y);
        flamability = space.getFlamability(x, y);
        
        intensityRate = flamability * flamability / 100;
        
        if(fuel == 0) {
            estimatedFireIntensity = intensity - energy - (int)intensityRate;
        } else {
            estimatedFireIntensity = intensity - energy + (int)intensityRate;
        }
        
        //If the agent can estimate that his spraying is not going to make a change
        //he might as well assume the next intensity is as high as possible since
        //he really shouldn't shoot it
        if(estimatedFireIntensity >= intensity) {
            estimatedFireIntensity = 100;
        }
        
        return estimatedFireIntensity;
    }
    
    public void draw(SimGraphics simGraphics) {
        simGraphics.drawRect(Color.GREEN);
        simGraphics.drawHollowRect(Color.BLUE);
        //simGraphics.drawCircle(Color.GREEN);
    }
    
    public int getVisibility() {
        return visibility;
    }
    
    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }
    
    public boolean searchBlackboard() {
        
        if (!this.taskToDo && blackboard.getSize() > 0) {
            Iterator<BlackboardMessage> taskList;
            BlackboardMessage task;
            
            int rand = random.nextInt(10);
            if (rand != 0) {
                // Search based on location (90% chance)
                int closest = space.getSize() * 2;
                int distance = 0;
                
                taskList = blackboard.iterator(0);
                while (taskList.hasNext()) {
                    task = taskList.next();
                    distance = Math.abs(this.getX() - task.getX()) + Math.abs(this.getY() - task.getY());
                    if (distance < closest) {
                        closest = distance;
                        this.task = task;
                        this.taskToDo = true;
                    }
                }
                
                if (this.taskToDo) {
                    blackboard.respond(this.task);
                    return true;
                }
                
            } else {
                // Search based on post age (10% chance)
                taskList = blackboard.iterator(0);
                while (taskList.hasNext()) {
                    this.task = taskList.next();
                    if(this.task.getFireStrength() < (getEnergy() * 1.5)) {
                        taskToDo = true;
                        blackboard.respond(this.task);
                        return true;
                    }
                }
            }
        }
        
        // Nothing found
        return false;
    }
}
