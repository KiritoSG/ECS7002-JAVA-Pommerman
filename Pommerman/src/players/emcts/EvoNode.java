package players.emcts;

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
 * Implements the evolutionary nodes which will be used in evolutionary MCTS algorithm.
 * Each child mutate from its parent, mutations are repaired with greedy action AI as described by Baier and Cowling.
 */
public class EvoNode
{
    // Public Variables:
    public EMCTSParams params;

    // Static Variables:
    private static final Random RANDOM = new Random(); // random seed
    private static final int GENOME_LEN = 5; // action sequence length

    // Other variables:
    private GameState rootGameState; // Fetches current situation from game to inform the algorithm of changes
    private StateHeuristic rootStateHeuristic;

    // Tree Node Variables: - One parent many children
    private EvoNode parent;
    private ArrayList<EvoNode> children; // Can append easily this way, may not search all possible children
    private ArrayList<Double> childrenScores; // Children scores, can be fetched by index as well
    private Types.ACTIONS[] actions;

    // Total value accumulated from
    private double totalValue;

    // Number of Visits made to this node.
    private int numberOfVisits;

    // Depth of the node:
    private int nodeDepth;

    // Bounds of the value we can get
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

    // Used for determining the computational budget
    private int fmCallsCount;

    // E-MCTS Variables:
    private Types.ACTIONS[] genome; // gives actual action actionBuffer hold int indices
    private boolean rndOpponentModel;

    // METHODS: ********************************************************************************************************
    // Constructor:
    /**
     * Initializes the EvoNode for MCTS algorithm.
     * @param params EMCTS tuning parameters
     * @param actions Actions available to the environment
     * @param parent Link to the parent node
     * @param fmCallsCount forward model call counts directed here
     * @param sh state heuristic function
     */
    EvoNode(EMCTSParams params, Types.ACTIONS[] actions, EvoNode parent, double totalValue, int numberOfVisits,
            int fmCallsCount, StateHeuristic sh, Types.ACTIONS[] genome, GameState gs) {

        this.params = params;
        this.actions = actions;

        this.parent = parent;
        children = new ArrayList<EvoNode>();

        this.totalValue = totalValue;
        this.numberOfVisits = numberOfVisits;

        this.fmCallsCount = fmCallsCount;

        this.rootGameState = gs;
        if (params.heuristic_method == params.CUSTOM_HEURISTIC)
            this.rootStateHeuristic = new CustomHeuristic(gs);
        else if (params.heuristic_method == params.ADVANCED_HEURISTIC) // New method: combined heuristics
            this.rootStateHeuristic = new AdvancedHeuristic(gs, RANDOM);

        // Initialize the depth we are in. If not null, inherit from parent + 1
        if(parent != null) {
            nodeDepth = parent.nodeDepth + 1;
            this.rootStateHeuristic = sh;
            this.genome = genome;
        }
        else {
            nodeDepth = 0;
            initializeRootGenome(rootGameState);
        }

    }

    //Initialization of genome - OSLA method ********************************************************************
    // TODO: Opponent modelling random -- Switch to OSLA.
    /**
     * Uses OSLA N times to create the root sequence. Opponents
     */
    private void initializeRootGenome(GameState gs) {

        GameState gsCopy = gs;
        genome = new Types.ACTIONS[GENOME_LEN];
        for (int i = 0; i < GENOME_LEN; i++) {
            genome[i] = predictOSLAAction(gsCopy);
        }
    }

    /**
     * Rolls the game state forward to see which action is the best.
     * @param gs Current GameState.
     * @return returns best action predicted by OSLA
     */
    private Types.ACTIONS predictOSLAAction(GameState gs) {

        rndOpponentModel = true;

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        double maxQ = Double.NEGATIVE_INFINITY;
        Types.ACTIONS bestAction = null;
        for (Types.ACTIONS act : actionsList) {
            GameState gsCopy = gs.copy();
            GameState rolledState = roll(gsCopy, act);
            double valState = rootStateHeuristic.evaluateState(gsCopy);
            // Other Variables:
            double epsilon = 1e-6;
            double Q = Utils.noise(valState, epsilon, this.RANDOM.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = act;
            }
        }
        return bestAction;

    }

    /**
     * Rolls the current game state to update the game state.
     * Roll to predict OSLA action or to actually move the game state.
     * @param gs The GameState fed to roll.
     * @param act List of actions available
     */
    private GameState roll(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];

        for(int i = 0; i < nPlayers; ++i)
        {
            if(i == gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey())
            {
                actionsAll[i] = act;
            }else{
                if(rndOpponentModel){
                    int actionIdx = RANDOM.nextInt(gs.nActions());
                    actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
                }else
                {
                    actionsAll[i] = Types.ACTIONS.ACTION_STOP;
                }
            }
        }

        gs.next(actionsAll);
        return gs; // returns game state again.
    }
    // *****************************************************************************************************************

    /**
     * Mutates a random action in the gene sequence.
     * @param genome Seqeuence to be mutated
     * @return Sequence which is mutated.
     */
    private Types.ACTIONS[] mutateGenome(Types.ACTIONS[] genome)
    {
        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();

        int genePosition = RANDOM.nextInt(genome.length);
        int mutation = RANDOM.nextInt(Types.ACTIONS.values().length);
        genome[genePosition] = this.actions[mutation];
        return genome;
    }

    // TODO: Repair of genome (Greedy) -- BUGGY!
    private Types.ACTIONS[] repairGenome(Types.ACTIONS[] genome, GameState gs) {

        GameState gsCopy = gs;
        boolean finished = false;
        int geneIdx = 0;
        Types.TILETYPE[][] board = gs.getBoard();
        int width = board.length;
        int height = board[0].length;

        while (!finished) {
            Types.ACTIONS action = genome[geneIdx];
            Vector2d dir = action.getDirection().toVec();

            Vector2d pos = gs.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            // gene is a valid gene - roll to get to the new state
            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    continue;

        }
        return null;
    }


    // *****************************************************************************************************************
    // TODO: MCTS Search Algorithms - Migrate from EvoNode - ISMCTS will be implemented here as well
    // TODO: Root, Tree or leaf parallelization can be done to speed up the process as well.
    void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;

        while(!stop){

            GameState state = rootGameState.copy();
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            EvoNode selected = treePolicy(state);
//            double delta = selected.rollOut(state); // SHALL NOT BE USED IN E-MCTS - EVAL LEAVES INSTEAD
//            backUp(selected, delta);

            //Stopping condition
            if(params.stop_type == params.STOP_TIME) {
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                fmCallsCount+=params.rollout_depth;
                stop = (fmCallsCount + params.rollout_depth) > params.num_fmcalls;
            }
        }
        //System.out.println(" ITERS " + numIters);
    }

    /**
     * Given a game state and current search situation decide whether the search should expand or current best
     * course of action should be exploited
     * @param state The current state of the game
     * @return Returns the node which expansion/evaluation will take place.
     */
    private EvoNode treePolicy(GameState state) {

    EvoNode cur = this;

    while (!state.isTerminal() && cur.nodeDepth < params.rollout_depth)
    {
        if (cur.notFullyExpanded()) {
            return cur.expand(state);

        } else {
            cur = cur.uct(state);
        }
    }

    return cur;
}

    /**
     * Takes the game state and expands the tree from its parent.
     * @param state The state of the game
     * @return returns a new child node which is expanded.
     */
    private EvoNode expand(GameState state) {

        Types.ACTIONS[] childGenome = genome;
        childGenome = mutateGenome(childGenome);
//        childGenome = mutateGenome(childGenome);

//        EvoNode(EMCTSParams params, Types.ACTIONS[] actions, EvoNode parent, double totalValue, int numberOfVisits,
//        int fmCallsCount, StateHeuristic sh, Types.ACTIONS[] genome, GameState gs) {

        EvoNode tn = new EvoNode(params, this.actions, this, 0,0, this.fmCallsCount,
                this.rootStateHeuristic, this.genome, this.rootGameState);
        children.get(bestAction) = tn;
        return tn;
    }

    private EvoNode uct(GameState state) {
        EvoNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (EvoNode child : this.children)
        {
            double hvVal = child.totalValue;
            double childValue =  hvVal / (child.numberOfVisits + params.epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

            double uctValue = childValue +
                    params.K * Math.sqrt(Math.log(this.numberOfVisits + 1) / (child.numberOfVisits + params.epsilon));

            uctValue = Utils.noise(uctValue, params.epsilon, this.RANDOM.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.size() + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        roll(state, actions[selected.childIdx]);

        return selected;
    }

    private void backUp(EvoNode node, double result)
    {
        EvoNode n = node;
        while(n != null)
        {
            n.numberOfVisits++;
            n.totalValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.size(); i++) {

            if(children.get(i) != null)
            {
                if(first == -1)
                    first = children.get(i).numberOfVisits;
                else if(first != children.get(i).numberOfVisits)
                {
                    allEqual = false;
                }

                double childValue = children.get(i).numberOfVisits;
                childValue = Utils.noise(childValue, params.epsilon, this.RANDOM.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    private int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i = 0; i < children.size(); i++) {

            if(children.get(i) != null) {
                double childValue = children.get(i).totalValue / (children.get(i).numberOfVisits + params.epsilon);
                childValue = Utils.noise(childValue, params.epsilon, this.RANDOM.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    private boolean notFullyExpanded() {
        for (EvoNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
}


