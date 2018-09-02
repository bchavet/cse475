/*
 * InfernoSpace.java
 *
 * Created on November 11, 2007, 6:25 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

import java.util.Random;
import uchicago.src.sim.space.Object2DGrid;

/**
 * This class models the environment.
 * 
 * @author Ben Chavet
 */
public class InfernoSpace {

    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int EAST = 3;
    public static final int WEST = 4;
    
    /**
     * Maximum allowed flamability level for a given space.
     */
    private int maxFlamability = 100;
    
    /**
     * Maximum allowed fire strength for a given space.
     */
    private int maxFireStrength = 100;
    
    /**
     * Track the fire strength for each space in the environment.
     */
    private Object2DGrid fireStrength;
    
    /**
     * Track the flamability of each space in the environment.  Once set, this
     * array should remain unchanged.
     */
    private Object2DGrid flamability;

    /**
     * Track how much fuel is remaining on each space in the environment.
     */
    private Object2DGrid fuelLevel;
    
    /**
     * Size of the environment
     */
    private int size;
    
    /**
     * Probability of ignition
     */
    private double ignitionProbability;
    
    /**
     * Random number generator
     */
    private Random random;
    
    /** 
     * Creates a new instance of InfernoSpace
     */
    public InfernoSpace(int size, Random rr) {
        this.size = size;
        this.random = rr;
        
        this.fireStrength = new Object2DGrid(this.size, this.size);
        this.flamability = new Object2DGrid(this.size, this.size);
        this.fuelLevel = new Object2DGrid(this.size, this.size);

        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                this.fireStrength.putValueAt(i, j, 0);
                this.flamability.putValueAt(i, j, 0);
                this.fuelLevel.putValueAt(i, j, 0);
            }
        }
}

    /** 
     * Updates the environmental values of all of the spaces in
     * the environment
     */
    public void updateSpace() {
        for (int i = 0; i < this.getSize(); i++) {
            for (int j = 0; j < this.getSize(); j++) {
                // Update the fire strength for this space.
                this.fireStrength.putValueAt(i, j, this.getNextFireStrength(i, j));

                // Update the fuel level for this space.
                this.fuelLevel.putValueAt(i, j, this.getNextFuelLevel(i, j));
            }
        }
    }

    /**
     * Get the flamability at the given space
     */
    public int getFlamability(int x, int y) {
        return (int)this.flamability.getValueAt(x, y);
    }
    
    public void setFlamability(int x, int y, int flamability) {
        this.flamability.putValueAt(x, y, flamability);
    }
    
    /**
     * Get the amount of fuel remaining at the given space.  Zero means that
     * the space cannot start on fire.
     */
    public double getFuelLevel(int x, int y) {
        return this.fuelLevel.getValueAt(x, y);
    }
    
    public void setFuelLevel(int x, int y, double fuelLevel) {
        this.fuelLevel.putValueAt(x, y, fuelLevel);
    }
    
    /**
     * Get the fire strength at the given space
     */
    public int getFireStrength(int x, int y) {
        return (int)this.fireStrength.getValueAt(x, y);
    }
    
    public void setFireStrength(int x, int y, int fireStrength) {
        this.fireStrength.putValueAt(x, y, fireStrength);
    }

    /**
     * Fight the fire at the given space with the given effectiveness.  The
     * effect on the fire depends on the given effectiveness as well as the
     * current fire strength of the space.
     *
     * @return int new fire strength
     **/
    public int fightFire(int x, int y, int effectiveness) {

        if (this.fireStrength.getValueAt(x, y) > 0) {
            this.fireStrength.putValueAt(x, y, this.fireStrength.getValueAt(x, y) - effectiveness);
            return (int)this.fireStrength.getValueAt(x, y);
        }
        
        // There was no fire to begin with.
        return 0;
    }
    
    /**
     * Calculate the fire strength for the next step.
     * There are three burn stages:
     *  1. Once the fire starts, it gets stronger until firestrength == flamability for the space
     *  2. The fire burns at a constant rate until the fuel is used up
     *  3. The fire burns out because it is out of fuel, at the same rate that it grew stronger
     */
    private int getNextFireStrength(int x, int y) {

        if (this.fireStrength.getValueAt(x, y) > 0) {
            // This space is already on fire
            
            if (this.fuelLevel.getValueAt(x, y) > 0) {
                // There is still fuel available at this space
                if (this.fireStrength.getValueAt(x, y) < this.maxFireStrength) {
                    // The fire is still growing in strength, increasing by flamability^2/maxFlamability
                    int increment = (int)Math.ceil(this.flamability.getValueAt(x, y) * this.flamability.getValueAt(x, y) / this.maxFlamability);
                    int newStrength = (int)this.fireStrength.getValueAt(x, y) + increment;
                    if (newStrength > this.maxFireStrength) {
                        return this.maxFireStrength;
                    }
                    return newStrength;
                    
                } else {
                    // The fire is at its max strength, there is no change
                    return (int)this.fireStrength.getValueAt(x, y);
                }
            } else {
                // There is no fuel remaining at this space, fire is burning out
                // Fire burns out at the same rate as it grew (flamability^2/maxFlamability)
                int decrement = (int)Math.ceil(this.flamability.getValueAt(x, y) * this.flamability.getValueAt(x, y) / this.maxFlamability);
                int newStrength = (int)this.fireStrength.getValueAt(x, y) - decrement;
                if (newStrength < 0) {
                    return 0;
                }
                return newStrength;
            }

        } else if (this.fireStrength.getValueAt(x, y) < 0) {
            // This space is not on fire, and is wet
            return (int)this.fireStrength.getValueAt(x, y) + 1;
            
        } else {
            // This space is not on fire.

            if (this.fuelLevel.getValueAt(x, y) > 0) {
                
                // There is fuel available at this space, check neighbors for fire
                int neighborFire = 0;
                if (y > 0) {
                    neighborFire += (int)this.fireStrength.getValueAt(x, y - 1); // North
                }
                if (y < this.getSize() - 1) {
                    neighborFire += (int)this.fireStrength.getValueAt(x, y + 1); // South
                }
                if (x < this.getSize() - 1) {
                    neighborFire += (int)this.fireStrength.getValueAt(x + 1, y); // East
                }
                if (x > 0) {
                    neighborFire += (int)this.fireStrength.getValueAt(x - 1, y); // West
                }
                
                // Ignite space if neighbor fire is >= 1 / flamability.
                // In other words, a space with high flamability will ignite
                // easier than a space with a low flamability.
                if (this.flamability.getValueAt(x, y) > 0) {
                    if (neighborFire >= this.maxFlamability - this.flamability.getValueAt(x, y)) {
                        int rand = random.nextInt((int)(1 / this.ignitionProbability));
                        if (rand == 0) {
                            return 1;
                        }
                        return 0;
                    }
                }
                
            }
        }

        return 0;
    }
    
    /**
     * Calculate the fuel level for the next step.
     */
    private double getNextFuelLevel(int x, int y) {
        // Fuel is consumed as a function of the fire strength.  so, at the fire's peak strenth, it will
        // use the most fuel in each step.

        if (this.fuelLevel.getValueAt(x, y) <= 0 || this.fireStrength.getValueAt(x, y) <= 0) {
            // If there is no fuel or no fire, there is no change.
            return this.fuelLevel.getValueAt(x, y);
        }
        return this.fuelLevel.getValueAt(x, y) - (this.fireStrength.getValueAt(x, y) / this.maxFireStrength);
    }

    /**
     * Returns whether or not there is still any fire in the environment
     */
    public boolean hasFire() {
        boolean fire = false;
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                if (this.fireStrength.getValueAt(i, j) > 0) {
                    fire = true;
                }
            }
        }
        return fire;
    }

    /**
     * Get the size of the environment.
     */
    public int getSize() {
        return size;
    }

    public int getMaxFlamability() {
        return maxFlamability;
    }

    public void setMaxFlamability(int maxFlamability) {
        this.maxFlamability = maxFlamability;
    }

    public double getIgnitionProbability() {
        return ignitionProbability;
    }

    public void setIgnitionProbability(double ignitionProbability) {
        this.ignitionProbability = ignitionProbability;
    }
    
    public Object2DGrid getFireStrengthObj() {
        return this.fireStrength;
    }
    
    public Object2DGrid getFlamabilityObj() {
        return this.flamability;
    }
    
    public Object2DGrid getFuelLevelObj() {
        return this.fuelLevel;
    }

    public int getMaxFireStrength() {
        return maxFireStrength;
    }

    public void setMaxFireStrength(int maxFireStrength) {
        this.maxFireStrength = maxFireStrength;
    }
}
