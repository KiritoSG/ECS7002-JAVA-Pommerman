package players.groupk;

import core.GameState;
import players.heuristics.AdvancedHeuristic;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;

import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.Random;

/**
 * Implements a single Evolutionary Node structure for EMCTS algorithm.
 */
public class EvoNode {

    // Static Variables: ***********************************************************************************************
    private static final Random RANDOM = new Random();      // Random initializer.
    private static final int GENOME_LENGTH  = 5;             // Length of the genome for this node.
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    // Private Node Variables: *****************************************************************************************
    private EvoNode parent;                                 // Each node have 1 parent, can have many children.
    private ArrayList<EvoNode> children;                    // Better than arrays, more flexible initialization.
    private Types.ACTIONS[] actions;                        // List of available actions for this node
    // Node Statistics: ************************************************************************************************
    private double totalValue;                              // Total Value accumulated from Node's children.
    private int numOfVisits;                                // Total visits accumulated from children.
    private int currentDepth;                               // Curr depth from the root node (incremented from parent).
    // Game Related Variables: *****************************************************************************************
    private GameState gameState;                            // Holds the game's current state while at current node.
    private StateHeuristic sh;                              // Holds the state heuristic function for this node.
    private EvoNodeParams params;                           // Holds parameters that contains EMCTS hyper-parameters.
    private int fmCallCount;                                // Holds how much FM Calls made so far until this node.
    // EMCTS Specific Variables: ***************************************************************************************
    private Types.ACTIONS[] geneSequence;                   // Gene sequence is hold in this genome.
    // *****************************************************************************************************************

    // Detailed Constructor: - Reserved for inner use. Too many parameters complicate Player's implementation.
    private EvoNode(EvoNode parent, ArrayList<EvoNode> children, Types.ACTIONS[] actions,
                    double totalValue, int numOfVisits,
                    EvoNodeParams params, int fmCallCount,
                    Types.ACTIONS[] geneSequence) {
        this.parent = parent;
        this.children = children;
        this.actions = actions;
        this.totalValue = totalValue;
        this.numOfVisits = numOfVisits;
        this.params = params;
        this.fmCallCount = fmCallCount;
        this.geneSequence = geneSequence;

        // Set the depth with respect to the parent. Similar implementation to MCTS
        if (parent != null) {
            this.currentDepth = parent.currentDepth + 1;
        } else {
            this.currentDepth = 0;
        }
    }

    // Visible Constructors: - Used by Player Algorithm; doesn't contain excess parameters.
    EvoNode(EvoNodeParams params, Types.ACTIONS[] actions) {
        this(null, new ArrayList<EvoNode>(), actions,
                0, 0,
                params, 0,
                new Types.ACTIONS[GENOME_LENGTH]);
    }

    EvoNode(EvoNodeParams params, Types.ACTIONS[] actions, Types.ACTIONS[] geneSequence) {
        this(null, new ArrayList<EvoNode>(), actions,
                0, 0,
                params, 0,
                geneSequence);
    }
    // *****************************************************************************************************************

    // Gene Mutators:
    private Types.ACTIONS[] mutateGenome(Types.ACTIONS[] genome) {
        int genePosition = RANDOM.nextInt(genome.length-1)+1;
        int mutation = RANDOM.nextInt(Types.ACTIONS.values().length);

        genome[genePosition] = this.actions[mutation];

        return genome;
    }

    public Types.ACTIONS[] repairGenome(GameState state, Types.ACTIONS[] genome) {

        int repairCheckIndex = 0;
        for (Types.ACTIONS act : genome) {
            GameState copy = state; // copy initial state each time.
            Types.ACTIONS[] gene = genome;

            // Roll until you reach to the genome you want to check.
            for (int i = 0; i <= repairCheckIndex; i++) {
                roll(copy, gene[i]);
            }

            // Check the genome and repair if needed:
            // Repair by trying all possible actions to take; if depleted you are checkmate.
            ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
            Types.TILETYPE[][] board = copy.getBoard();
            int width = board.length;
            int height = board[0].length;

            Vector2d dir = act.getDirection().toVec();
            Vector2d pos = copy.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            // If within bounds:
            if (x >= 0 && x < width && y >= 0 && y < height)
                // If  touching flames, do trials, else leave as is.:
                if (board[y][x] == Types.TILETYPE.FLAMES) {

                    while (actionsToTry.size() > 0) {

                        int nAction = RANDOM.nextInt(actionsToTry.size());
                        Types.ACTIONS trial = actionsToTry.get(nAction);

                        Vector2d dir1 = trial.getDirection().toVec();
                        Vector2d pos1 = state.getPosition();

                        int x1 = pos1.x + dir1.x;
                        int y1 = pos1.y + dir1.y;

                        if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height)
                            if (board[y1][x1] != Types.TILETYPE.FLAMES)
                                genome[repairCheckIndex] = actions[nAction];

                        actionsToTry.remove(nAction); // didn't work proceed with next one.
                    }
                    // nothing works mutate randomly and send it.
                    genome[repairCheckIndex] = predictOSLAAction(copy);
//                            actions[RANDOM.nextInt(actions.length)];
                    repairCheckIndex++;
                }
        }
//        System.out.print(genome[0]);
//        System.out.print(',');
//        System.out.print(genome[1]);
//        System.out.print(',');
//        System.out.print(genome[2]);
//        System.out.println();

        return genome;
    }

    // *****************************************************************************************************************

    // Setters & Initializers:
    void setGameState(GameState gs) {
        // From EvoNode: Renamed as this method is for Nodes; not particularly a parent node.

        // Used only a single heuristic; removed redundant statements.
        this.sh = new AdvancedHeuristic(gs, RANDOM);
        this.gameState = gs;

//        this.sh = new CustomHeuristic(gs);
    }

    public void initializeRootGenome(GameState gs) {

        GameState gsCopy = gs;
        geneSequence = new Types.ACTIONS[GENOME_LENGTH];
        for (int i = 0; i < GENOME_LENGTH; i++) {
            geneSequence[i] = predictOSLAAction(gsCopy);
            // No copy of GameState used inside this method; hence rolls are permanent (for gsCopy)
        }
        geneSequence = repairGenome(gs, geneSequence);
//        System.out.println(geneSequence[0].toString() +
//                ',' + geneSequence[1].toString() +
//                ',' + geneSequence[2].toString() +
//                ',' + geneSequence[3].toString() +
//                ',' + geneSequence[4].toString());
    }

    public Types.ACTIONS predictOSLAAction(GameState gs) {
        GameState copy = gs;
        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        double maxQ = Double.NEGATIVE_INFINITY;
        Types.ACTIONS bestAction = null;
        for (Types.ACTIONS act : actionsList) {
            roll(copy, act);
            double valState = sh.evaluateState(copy);

            // Other Variables:
            double Q = Utils.noise(valState, params.epsilon, this.RANDOM.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = act;
            }
        }
        return bestAction;

    }
    // *****************************************************************************************************************

    // MCTS Search: ****************************************************************************************************
    public void MCTSSearch(ElapsedCpuTimer elapsedTimer) {
        boolean stop = false;
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;

        // Select and Expand:
        while (!stop) {
            // Each MCTS Search call should be confined to game state of the current node.
            // As evaluation needs rolling of gameState; work on copy to hold onto original state.
            GameState state = gameState.copy();
            // Set timer to measure how well the algorithm does. Taken from EvoNode.
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            // Select a child node to expand or evaluate & update values.
            // More explanation within comments of this code segment.

            selectChildNode(state);

            // Backpropagation of child scores to parent:
            backUp();

            // Stopping conditions - Taken from SingleTreeNode -- MCTSParams migrated too! (Uses MCTS for now)
            if (params.stop_type == params.STOP_TIME) {
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
                avgTimeTaken = acumTimeTaken / numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;

            }
            else if (params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;

            } else if (params.stop_type == params.STOP_FMCALLS) {
                fmCallCount += params.MAX_ROLLOUTS;
                stop = (fmCallCount + params.MAX_ROLLOUTS) > params.num_fmcalls;
            }
        }
    }

    void selectChildNode(GameState state) {

        while (!state.isTerminal() && this.currentDepth < EvoNodeParams.MAX_ROLLOUTS) {
            // Expand => go further down the tree
            if (this.notFullyExpanded()) {
                this.expand(state);
            }
            // Done expanding, evaluate leaf nodes
            else {
                this.evaluate(state);
                return;
            }
        }
//        return current;
    }

    private boolean notFullyExpanded() {
        // Only allow parents to expand certain amount of children.
        // With genome approach branching factor is too large; one cannot try all combinations.
        return children.size() < EvoNodeParams.BRANCH_FACTOR;
    }

    private void expand(GameState gs) {
        // Randomly pick a gene to mutate
        Types.ACTIONS[] childSequence = mutateGenome(geneSequence);
        childSequence = repairGenome(gs, childSequence);

        // Do not roll state yet, this is not a single expansion. The states will be rolled in order.

        EvoNode childNode = new EvoNode(this, new ArrayList<EvoNode>(), this.actions,
                0, 0,
                this.params, this.fmCallCount,
                childSequence);
        childNode.sh = this.sh;
        childNode.setGameState(gs);
        children.add(childNode);
//        return childNode;
    }

    private void evaluate(GameState state) {
        double bestValue = -Double.MAX_VALUE;

        // For each of the children do the following:
        for (EvoNode child : children) {
            // Copy game state, to avoid losing the game's current state.
            GameState copy = state.copy();
            child.numOfVisits++;

            // For each of action in the child's sequence do a rollout; reward is cumulative for that state.
            // Rollout opponent modelling is random;
            // this also creates more problems across different runs of the same agent sequence.

            double result = 0;
            for (Types.ACTIONS action : child.geneSequence) {
                roll(copy, action);   // Taken from MCTS implementation
                result = Utils.noise(sh.evaluateState(copy), params.epsilon, RANDOM.nextDouble());
//                System.out.println(result);
                child.totalValue += result;
//                child.totalValue = child.totalValue/child.numOfVisits;
//                System.out.println("total value " + child.totalValue);
                if (copy.isTerminal()) {
                    child.totalValue = child.totalValue / child.numOfVisits;
                    break;
                }


//            result = Utils.noise(sh.evaluateState(copy), params.epsilon, RANDOM.nextDouble());
//            child.totalValue += result;

                // UCT Calculation from SingleTreeNode:
//                double hvVal = child.totalValue;
//                double childValue = hvVal / (child.numOfVisits + params.epsilon);
////
//                childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
//
//                double uctValue = childValue +
//                        params.K * Math.sqrt(Math.log(this.numOfVisits + 1) / (child.numOfVisits + params.epsilon));
//
//                uctValue = Utils.noise(uctValue, params.epsilon, RANDOM.nextDouble());

                if (result> bestValue) { // result <-- uctValue to disregard UCT
//                    best = child;
                    bestValue = result;
                }
            }
        }

//        return best;
    }

    private void roll(GameState gs, Types.ACTIONS act) {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        for (int i = 0; i < nPlayers; ++i) {
            if (playerId == i) {
                actionsAll[i] = act;
            } else {
                int actionIdx = RANDOM.nextInt(gs.nActions());
                actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }
        gs.next(actionsAll);
    }

    private void backUp() {
        // For each of the child that a node as back up the visits and values.
        for (EvoNode child : children) {
            this.totalValue += child.totalValue;
            this.numOfVisits += child.numOfVisits;
        }
        this.totalValue = this.totalValue / this.numOfVisits;
    }

    public EvoNode findBestChildren() {
        // Loop over the score board and get the best genome
//        Types.ACTIONS[] bestGenome = new Types.ACTIONS[GENOME_LENGTH];
        double bestValue = -Double.MAX_VALUE;
        EvoNode bestChild = null;
        if (children.size() != 0) {
            for (EvoNode child : children) {
                if (child.totalValue > bestValue) {
                    bestValue = child.totalValue;
//                    bestGenome = child.geneSequence;
                    bestChild = child;
                }
            }
        } else {
//            System.out.println("No children exist. Returning this.");
            return this;
        }

//        for (EvoNode child: children) {
//            System.out.println();
//            System.out.println(child.totalValue);
//            System.out.println(child.numOfVisits);
//        }

        return bestChild;
    }

    // Player Support methods:
    public Types.ACTIONS[] findBestGenome() {
        // Loop over the score board and get the best genome
        Types.ACTIONS[] bestGenome = new Types.ACTIONS[GENOME_LENGTH];
        double bestValue = -Double.MAX_VALUE;

        if (children.size() != 0) {
            for (EvoNode child : children) {
                if (child.totalValue > bestValue) {
                    bestValue = child.totalValue;
                    bestGenome = child.geneSequence;
                }
            }
        } else {
            return this.geneSequence; // no children exist
        }
        return bestGenome; // a best solution found.
    }

    public Types.ACTIONS[] getGeneSequence() {
        return geneSequence;
    }

    public void setGeneSequence(Types.ACTIONS[] geneSequence) {
        this.geneSequence = geneSequence;
    }
    // *****************************************************************************************************************
}
