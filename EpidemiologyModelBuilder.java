package epidemiology;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;

import java.util.ArrayList;
import java.util.List;

public class EpidemiologyModelBuilder implements ContextBuilder<Object> {

    @Override
    public Context<Object> build(Context<Object> context) {
        context.setId("epidemiology");

        // Barabási-Albert Network Parameters
        int initialNodes = 3;  // Initial fully connected network size
        int totalNodes = 0;    // Total number of nodes to generate
        int edgesPerNewNode = 2; // Number of edges to attach from a new node to existing nodes

        // Network Builder
        NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("infection network", context, true);
        Network<Object> network = netBuilder.buildNetwork();

        ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
                .createContinuousSpaceFactory(null);
        ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
                "space", context, new RandomCartesianAdder<Object>(),
                new repast.simphony.space.continuous.WrapAroundBorders(), 30,
                30);

        GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
        Grid<Object> grid = gridFactory.createGrid("grid", context,
                new GridBuilderParameters<Object>(new WrapAroundBorders(),
                        new SimpleGridAdder<Object>(), true, 30, 30));

        Parameters params = RunEnvironment.getInstance().getParameters();

        // Get simulation parameters
        double beta = (Double) params.getValue("beta");
        double gamma = (Double) params.getValue("gamma");
        int zombieCount = (Integer) params.getValue("infected_count");
        int humanCount = (Integer) params.getValue("susceptible_count");
        int recoveredCount = (Integer) params.getValue("recovered_count");
        
        // Determine total nodes based on simulation parameters
        totalNodes = zombieCount + humanCount + recoveredCount;

        // Create initial fully connected network
        List<Object> initialNodesList = new ArrayList<Object>();
        for (int i = 0; i < Math.min(initialNodes, totalNodes); i++) {
            Object node;
            if (i < zombieCount) {
                node = new Infected(space, grid, beta, gamma);
            } else if (i < zombieCount + humanCount) {
                int energy = RandomHelper.nextIntFromTo(4, 10);
                node = new Susceptible(space, grid, energy);
            } else {
                node = new Recovered(space, grid);
            }
            context.add(node);
            initialNodesList.add(node);
        }

        // Connected initial network
        for (int i = 0; i < initialNodesList.size(); i++) {
            for (int j = i + 1; j < initialNodesList.size(); j++) {
                network.addEdge(initialNodesList.get(i), initialNodesList.get(j));
            }
        }

        // Barabási-Albert Network
        for (int i = initialNodes; i < totalNodes; i++) {
            Object newNode;
            if (i < zombieCount) {
                newNode = new Infected(space, grid, beta, gamma);
            } else if (i < zombieCount + humanCount) {
                int energy = RandomHelper.nextIntFromTo(4, 10);
                newNode = new Susceptible(space, grid, energy);
            } else {
                newNode = new Recovered(space, grid);
            }
            context.add(newNode);

            // Attach edges based on preferential attachment
            List<Object> existingNodes = new ArrayList<Object>();
            existingNodes.removeAll(initialNodesList);

            // Compute node degrees
            int[] nodeDegrees = new int[existingNodes.size()];
            int totalDegree = 0;
            for (int j = 0; j < existingNodes.size(); j++) {
                nodeDegrees[j] = network.getDegree(existingNodes.get(j));
                totalDegree += nodeDegrees[j];
            }

            // Attach edges
            int edgesAttached = 0;
            while (edgesAttached < edgesPerNewNode && edgesAttached < existingNodes.size()) {
                // Preferential attachment: select node with probability proportional to its degree
                double randVal = RandomHelper.nextDouble() * totalDegree;
                int selectedNodeIndex = -1;
                double cumulativeDegree = 0;

                for (int j = 0; j < existingNodes.size(); j++) {
                    cumulativeDegree += nodeDegrees[j];
                    if (randVal <= cumulativeDegree) {
                        selectedNodeIndex = j;
                        break;
                    }
                }

                if (selectedNodeIndex != -1) {
                    Object targetNode = existingNodes.get(selectedNodeIndex);
                    
                    // Prevent duplicate edges
                    if (!network.isAdjacent(newNode, targetNode)) {
                        network.addEdge(newNode, targetNode);
                        edgesAttached++;
                    }
                }
            }
        }

        // Rest of the original method remains the same
        for (Object obj : context.getObjects(Object.class)) {
            NdPoint pt = space.getLocation(obj);
            grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
        }
        
        if (RunEnvironment.getInstance().isBatch()) {
            RunEnvironment.getInstance().endAt(20);
        }

        return context;
    }
}
/*

package epidemiology;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class EpidemiologyModelBuilder implements ContextBuilder<Object> {

    @Override
    public Context build(Context<Object> context) {
        context.setId("epidemiology");

        // Create continuous space
        ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
                .createContinuousSpaceFactory(null);
        ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
                "space", context, new RandomCartesianAdder<>(),
                new repast.simphony.space.continuous.WrapAroundBorders(), 30, 30);

        // Create grid
        GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
        Grid<Object> grid = gridFactory.createGrid("grid", context,
                new GridBuilderParameters<>(new WrapAroundBorders(),
                        new SimpleGridAdder<>(), true, 30, 30));

        // Create small-world network
        NetworkBuilder<Object> netBuilder = new NetworkBuilder<>("infection network", context, true);
        Network<Object> network = netBuilder.buildNetwork();
        createSmallWorldNetwork(network, context, 3); // Small-world rewiring probability

        // Retrieve parameters
        Parameters params = RunEnvironment.getInstance().getParameters();
        double beta = (Double) params.getValue("beta");
        double gamma = (Double) params.getValue("gamma");
        int infectedCount = (Integer) params.getValue("infected_count");
        int susceptibleCount = (Integer) params.getValue("susceptible_count");
        int recoveredCount = (Integer) params.getValue("recovered_count");

        // Add agents
        for (int i = 0; i < infectedCount; i++) {
            context.add(new Infected(space, grid, beta, gamma));
        }

        for (int i = 0; i < susceptibleCount; i++) {
            int energy = RandomHelper.nextIntFromTo(4, 10);
            context.add(new Susceptible(space, grid, energy));
        }

        for (int i = 0; i < recoveredCount; i++) {
            context.add(new Recovered(space, grid));
        }

        // Position agents on the grid
        for (Object obj : context.getObjects(Object.class)) {
            NdPoint pt = space.getLocation(obj);
            grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
        }

        // Set simulation end conditions
        if (RunEnvironment.getInstance().isBatch()) {
            RunEnvironment.getInstance().endAt(20);
        }

        return context;
    }
    */
    

    /**
     * Creates a small-world network using the Watts-Strogatz model.
     *
     * @param network the network to modify
     * @param context the simulation context containing agents
     * @param rewiringProbability the probability of rewiring each edge
     */
    
    /*
    private void createSmallWorldNetwork(Network<Object> network, Context<Object> context, double rewiringProbability) {
        List<Object> agents = new ArrayList<>();
        for (Object obj : context.getObjects(Object.class)) {
            agents.add(obj);
        }

        int k = 4; // Number of initial neighbors (must be even)
        int numAgents = agents.size();

        // Create a ring lattice
        for (int i = 0; i < numAgents; i++) {
            for (int j = 1; j <= k / 2; j++) {
                int neighborIndex = (i + j) % numAgents;
                network.addEdge(agents.get(i), agents.get(neighborIndex));
            }
        }

        // Rewire edges with the given probability
        Random random = new Random();
        for (Object source : agents) {
            List<Object> neighbors = new ArrayList<>();
            for (RepastEdge<Object> edge : network.getOutEdges(source)) {
                neighbors.add(edge.getTarget());
            }

            for (Object target : neighbors) {
                if (random.nextDouble() < rewiringProbability) {
                    // Retrieve the edge between source and target
                    RepastEdge<Object> edgeToRemove = network.getEdge(source, target);

                    if (edgeToRemove != null) {
                        // Remove existing edge
                        network.removeEdge(edgeToRemove);

                        // Create a new edge to a random node
                        Object newTarget;
                        do {
                            newTarget = agents.get(random.nextInt(numAgents));
                        } while (newTarget.equals(source) || network.isAdjacent(source, newTarget));

                        network.addEdge(source, newTarget);
                    }
                }
            }
        }

        }
         
    

    
    
    
}
*/

    
    
    
