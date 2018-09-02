/*
 * InfernoModel.java
 *
 * Created on November 24, 2007, 1:41 PM
 *
 * CSE475 Project
 * Team Win: Ben Chavet, Justin McKinstry, Steve Mott
 */

package inferno;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Random;
import java.util.ListIterator;
import java.awt.Color;
import java.util.Collections;
import java.lang.Math;

import uchicago.src.sim.space.*;
import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.*;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.analysis.*;
import cern.jet.random.Uniform;
import cern.jet.random.Normal;
import uchicago.src.reflector.BooleanPropertyDescriptor;

/**
 *
 * @author Ben Chavet
 */
public class InfernoModel extends SimpleModel {
    
    private int numberOfFires = 3;
    private int numberOfFirefighters = 10;
    private int maxFlamability = 100;
    private int maxFuelLevel = 100;
    private int maxFireStrength = 100;
    private double ignitionProbability = .01;
    private int gridSize = 100;
    private int agentEnergy = 10;
    private int agentVisibility = 15;
    private int agentExtinguishBonus = 1;
    private int agentShotStrengthPercent = 100;
    private boolean showFlamability = true;
    private boolean showFuelLevel = true;
    
    private double initialFuelLevel = 0;
    
    private InfernoSpace grid;
    private Blackboard blackboard;
    
    private ArrayList<InfernoAgent> firefighterList;
    private ArrayList<InfernoAgent> killedInAction;

    private Random random;
    
    private DisplaySurface fireStrengthDisplaySurface;
    private DisplaySurface flamabilityDisplaySurface;
    private DisplaySurface fuelLevelDisplaySurface;

    private Multi2DGrid agentGrid;
    
    /**
     * Creates a new instance of InfernoModel
     */
    public InfernoModel() {
        name = "Inferno Model";
        params = new String[] { "numberOfFires", "numberOfFirefighters", "maxFlamability", "maxFuelLevel", "maxFireStrength", "gridSize", "ignitionProbability", "agentEnergy", "agentVisibility", "agentExtinguishBonus", "agentShotStrengthPercent", "showFlamability", "showFuelLevel"};
    }
    
    /**
     * Set up the simulation
     */
    public void setup() {
        super.setup();
        
        // Initialize the display surfaces
        if (this.fireStrengthDisplaySurface != null) {
            this.fireStrengthDisplaySurface.dispose();
        }
        this.fireStrengthDisplaySurface = new DisplaySurface(this, "Fire Strength");
        registerDisplaySurface("Fire Strength", this.fireStrengthDisplaySurface);

        if (this.showFlamability) {
            if (this.flamabilityDisplaySurface != null) {
                this.flamabilityDisplaySurface.dispose();
            }
            this.flamabilityDisplaySurface = new DisplaySurface(this, "Flamability");
            registerDisplaySurface("Flamability", this.flamabilityDisplaySurface);
        }
        
        if (this.showFuelLevel) {
            if (this.fuelLevelDisplaySurface != null) {
                this.fuelLevelDisplaySurface.dispose();
            }
            this.fuelLevelDisplaySurface = new DisplaySurface(this, "Fuel Level");
            registerDisplaySurface("Fuel Level", this.fuelLevelDisplaySurface);
        }
    }
    
    /**
     * Builds the agents and the environment
     */
    public void buildModel() {
        
        // Initialize random number generator
        this.random = new Random(this.getRngSeed());

        // Initialize the environment
        this.grid = new InfernoSpace(this.getGridSize(), this.random);
        this.grid.setMaxFlamability(this.getMaxFlamability());
        this.grid.setMaxFireStrength(this.getMaxFireStrength());
        this.grid.setIgnitionProbability(this.getIgnitionProbability());
        
        // Initialize the blackboard
        this.blackboard = new Blackboard();
        
        // Set up uniform random generator
        
        //uchicago.src.sim.util.Random.createUniform();
        int randX;
        int randY;
        int randVal;
        
        // Initialize the flamability levels
        for (int i = 0; i < this.getGridSize(); i++) {
            for (int j = 0; j < this.getGridSize(); j ++) {
                randVal = random.nextInt(this.getMaxFlamability());
                this.grid.setFlamability(i, j, randVal);
            }
        }
        
        // Initialize the fuel levels
        this.initialFuelLevel = 0;
        for (int i = 0; i < this.getGridSize(); i++) {
            for (int j = 0; j < this.getGridSize(); j++) {
                randVal = random.nextInt(this.getMaxFuelLevel());
                this.grid.setFuelLevel(i, j, randVal);
                this.initialFuelLevel = this.initialFuelLevel + randVal;
            }
        }
        
        // Initialize the fires
        for (int i = 0; i < this.getNumberOfFires(); i++) {
            do {
                randX = random.nextInt(this.getGridSize());
                randY = random.nextInt(this.getGridSize());
            } while (grid.getFlamability(randX, randY) <= 0);
            this.grid.setFireStrength(randX, randY, 1);
        }
        
        // Initialize the firefighter tracker (for display purposes)
        this.agentGrid = new Multi2DGrid(this.getGridSize(), this.getGridSize(), true);
        
        // Initialize the firefighter agents
        this.firefighterList = new ArrayList<InfernoAgent>();
        this.killedInAction = new ArrayList<InfernoAgent>();
        for (int i = 0; i < this.getNumberOfFirefighters(); i++) {
            do {
                randX = random.nextInt(this.getGridSize());
                randY = random.nextInt(this.getGridSize());
            } while (grid.getFireStrength(randX, randY) > 0);
            InfernoAgent firefighter = new InfernoAgent(this.grid, this.blackboard, random);
            firefighter.move(randX, randY);
            firefighter.setEnergy(this.agentEnergy);
            firefighter.setVisibility(this.agentVisibility);
            firefighter.setEnergyGainModifier(this.agentExtinguishBonus);
            firefighter.setShotStrengthModifier(this.agentShotStrengthPercent);
            this.firefighterList.add(firefighter);
            
            this.agentGrid.putObjectAt(randX, randY, firefighter);
        }
        
        buildDisplay();
        
        fireStrengthDisplaySurface.display();
        fireStrengthDisplaySurface.setSize(fireStrengthDisplaySurface.getDefaultSize());
        
        if (this.showFlamability) {
            flamabilityDisplaySurface.display();
            flamabilityDisplaySurface.setSize(flamabilityDisplaySurface.getDefaultSize());
        }
        
        if (this.showFuelLevel) {
            fuelLevelDisplaySurface.display();
            fuelLevelDisplaySurface.setSize(fuelLevelDisplaySurface.getDefaultSize());
        }
    }
    
    public void buildDisplay() {

        MultiObject2DDisplay agentGridDisplay = new MultiObject2DDisplay(this.agentGrid);
        
        ColorMap fireStrengthMap = new ColorMap();
        fireStrengthMap.mapColor(0, new Color(0, 180, 0));
        for (int i = 1; i <= this.getMaxFireStrength(); i++) {
            int color = 255 - (int)(255 * i / this.getMaxFireStrength());
            fireStrengthMap.mapColor(i, new Color(255, color, color));
        }
        
        Value2DDisplay fireStrengthDisplay = new Value2DDisplay(this.grid.getFireStrengthObj(), fireStrengthMap);
        fireStrengthDisplaySurface.addDisplayableProbeable(fireStrengthDisplay, "Fire Strength");
        fireStrengthDisplaySurface.addDisplayableProbeable(agentGridDisplay, "Firefighters");

        addSimEventListener(fireStrengthDisplaySurface);

        if (this.showFlamability) {
            ColorMap flamabilityMap = new ColorMap();
            for (int i = 0; i <= this.getMaxFlamability(); i++) {
                int color = (int)(255 * i / this.getMaxFlamability());
                flamabilityMap.mapColor(i, new Color(color, color, color));
            }

            Value2DDisplay flamabilityDisplay = new Value2DDisplay(this.grid.getFlamabilityObj(), flamabilityMap);
            flamabilityDisplaySurface.addDisplayableProbeable(flamabilityDisplay, "Flamability");
            flamabilityDisplaySurface.addDisplayableProbeable(agentGridDisplay, "Firefighters");

            addSimEventListener(flamabilityDisplaySurface);
        }
        
        if (this.showFuelLevel) {
            ColorMap fuelLevelMap = new ColorMap();
            for (int i = 0; i <= this.getMaxFuelLevel(); i++) {
                int color = (int)(255 * i / this.getMaxFuelLevel());
                fuelLevelMap.mapColor(i, new Color(color, color, color));
            }
        
            Value2DDisplay fuelLevelDisplay = new Value2DDisplay(this.grid.getFuelLevelObj(), fuelLevelMap);
            fuelLevelDisplaySurface.addDisplayableProbeable(fuelLevelDisplay, "Fuel Level");
            fuelLevelDisplaySurface.addDisplayableProbeable(agentGridDisplay, "Firefighters");

            addSimEventListener(fuelLevelDisplaySurface);
        }
        
    }
    
    /**
     * Controls what is done during each tick of the simulation.
     */
    public void step() {
        // Update the environment
        this.grid.updateSpace();

        // Update the firefighter agents
        for (InfernoAgent agent : this.firefighterList) {
            this.agentGrid.removeObjectAt(agent.getX(), agent.getY(), agent);
            agent.step();
            if (agent.getEnergy() > 0) {
                this.agentGrid.putObjectAt(agent.getX(), agent.getY(), agent);
            } else {
                killedInAction.add(agent);
            }
        }
        
        // Remove any killed firefighters from the list
        for (InfernoAgent agent : killedInAction) {
            this.firefighterList.remove(agent);
            //System.out.println("Firefighter Killed, " + this.firefighterList.size() + " remaining.");
        }
        
        // Update the displays
        this.fireStrengthDisplaySurface.updateDisplay();
        
        if (this.showFlamability) {
            this.flamabilityDisplaySurface.updateDisplay();
        }
        
        if (this.showFuelLevel) {
            this.fuelLevelDisplaySurface.updateDisplay();
        }
        
        // Check for end conditions
        if (!this.grid.hasFire()) {
            stop();
        }
    }
    
    /**
     * Clean up, print results, and end simulation
     */
    public void stop() {
        double percentFirefightersRemaining;
        double percentHealthRemaining;
        double percentFuelRemaining;
        
        System.out.println("-----");
        
        // Print initial values.  This is pretty much the "fingerprint" of this run
        System.out.println(this.getAgentEnergy() + "," + this.getAgentExtinguishBonus() + "," + this.getAgentShotStrengthPercent() + "," + this.getAgentVisibility() + "," + this.getGridSize() + "," + this.getIgnitionProbability() + "," + this.getMaxFireStrength() + "," + this.getMaxFlamability() + "," + this.getMaxFuelLevel() + "," + this.getNumberOfFirefighters() + "," + this.getNumberOfFires() + "," + this.getRngSeed());
        
        // Calculate number of remaining firefighters
        try {
            percentFirefightersRemaining = 100 * this.firefighterList.size() / this.getNumberOfFirefighters();
        } catch (ArithmeticException e) {
            // Check for division by zero
            percentFirefightersRemaining = 0;
        }
        System.out.println(this.firefighterList.size() + " / " + this.getNumberOfFirefighters() + " firefighters still alive (" + percentFirefightersRemaining + "%)");
        
        // Calculate total remaining firefighter health
        int totalHealth = 0;
        for (InfernoAgent agent : this.firefighterList) {
            totalHealth = totalHealth + agent.getEnergy();
        }
        try {
            percentHealthRemaining = 100 * totalHealth / (this.getNumberOfFirefighters() * this.getAgentEnergy());
        } catch (ArithmeticException e) {
            // Check for division by zero
            percentHealthRemaining = 0;
        }
        System.out.println(totalHealth + " / " + (this.getNumberOfFirefighters() * this.getAgentEnergy()) + " total remaining firefighter health (" + percentHealthRemaining + "%)");
        
        // Calculate each firefighter's remaining health
        int count = 0;
        for (InfernoAgent agent : this.firefighterList) {
            System.out.println("  Firefighter " + count++ + ": " + agent.getEnergy());
        }        
        for (InfernoAgent agent : this.killedInAction) {
            System.out.println("  Firefighter " + count++ + ": " + agent.getEnergy());
        }
        
        // Calculate total remaining fuel
        double totalFuel = 0;
        for (int i = 0; i < this.grid.getSize(); i++) {
            for (int j = 0; j < this.grid.getSize(); j++) {
                totalFuel = totalFuel + this.grid.getFuelLevel(i, j);
            }
        }
        try {
            percentFuelRemaining = 100 * totalFuel / this.initialFuelLevel;
        } catch (ArithmeticException e) {
            // Check for division by zero
            percentFuelRemaining = 0;
        }
        System.out.println(totalFuel + " / " + this.initialFuelLevel + " total fuel remaining (" + percentFuelRemaining + "%)");

        // Stop simulation
        this.fireStopSim();
    }
    
    /**
     * Main Method, starts simulation
     */
    public static void main(String[] args) {
        SimInit init = new SimInit();
        InfernoModel model = new InfernoModel();
        init.loadModel(model, "", false);
    }
    
    public int getNumberOfFires() {
        return numberOfFires;
    }
    
    public void setNumberOfFires(int numberOfFires) {
        this.numberOfFires = numberOfFires;
    }
    
    public int getGridSize() {
        return gridSize;
    }
    
    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }
    
    public int getMaxFlamability() {
        return maxFlamability;
    }
    
    public void setMaxFlamability(int maxFlamability) {
        this.maxFlamability = maxFlamability;
    }

    public int getNumberOfFirefighters() {
        return numberOfFirefighters;
    }

    public void setNumberOfFirefighters(int numberOfFirefighters) {
        this.numberOfFirefighters = numberOfFirefighters;
    }

    public int getMaxFuelLevel() {
        return maxFuelLevel;
    }

    public void setMaxFuelLevel(int maxFuelLevel) {
        this.maxFuelLevel = maxFuelLevel;
    }

    public double getIgnitionProbability() {
        return ignitionProbability;
    }

    public void setIgnitionProbability(double ignitionProbability) {
        this.ignitionProbability = ignitionProbability;
    }

    public int getAgentEnergy() {
        return agentEnergy;
    }

    public void setAgentEnergy(int agentEnergy) {
        this.agentEnergy = agentEnergy;
    }

    public int getAgentExtinguishBonus() {
        return agentExtinguishBonus;
    }

    public void setAgentExtinguishBonus(int agentExtinguishBonus) {
        this.agentExtinguishBonus = agentExtinguishBonus;
    }

    public int getAgentShotStrengthPercent() {
        return agentShotStrengthPercent;
    }

    public void setAgentShotStrengthPercent(int agentShotStrengthPercent) {
        this.agentShotStrengthPercent = agentShotStrengthPercent;
    }

    public boolean isShowFlamability() {
        return showFlamability;
    }

    public void setShowFlamability(boolean showFlamability) {
        this.showFlamability = showFlamability;
    }

    public boolean isShowFuelLevel() {
        return showFuelLevel;
    }

    public void setShowFuelLevel(boolean showFuelLevel) {
        this.showFuelLevel = showFuelLevel;
    }

    public int getAgentVisibility() {
        return agentVisibility;
    }

    public void setAgentVisibility(int agentVisibility) {
        this.agentVisibility = agentVisibility;
    }

    public int getMaxFireStrength() {
        return maxFireStrength;
    }

    public void setMaxFireStrength(int maxFireStrength) {
        this.maxFireStrength = maxFireStrength;
    }
}
