package players.mcts;

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

public class mySingleTreeNode {
    public MCTSParams params;

    private mySingleTreeNode parent;
    private mySingleTreeNode[] children;
    private double totValue;
    private int nVisits;
    private Random m_rnd;
    private int m_depth;
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    private int childIdx;
    private int fmCallsCount;

    private int num_actions;
    private Types.ACTIONS[] actions;

    private GameState rootState;
    private StateHeuristic rootStateHeuristic;

    mySingleTreeNode(MCTSParams p, Random rnd, int num_actions, Types.ACTIONS[] actions) {
        this(p, null, -1, rnd, num_actions, actions, 0, null);
    }

    private mySingleTreeNode(MCTSParams p, mySingleTreeNode parent, int childIdx, Random rnd, int num_actions,
                           Types.ACTIONS[] actions, int fmCallsCount, StateHeuristic sh) {
        this.params = p;
        this.fmCallsCount = fmCallsCount;
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        children = new mySingleTreeNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null) {
            m_depth = parent.m_depth + 1;
            this.rootStateHeuristic = sh;
        }
        else
            m_depth = 0;
    }

    void setRootGameState(GameState gs)
    {
        this.rootState = gs;
        if (params.heuristic_method == params.CUSTOM_HEURISTIC)
            this.rootStateHeuristic = new CustomHeuristic(gs);
        else if (params.heuristic_method == params.ADVANCED_HEURISTIC) // New method: combined heuristics
            this.rootStateHeuristic = new AdvancedHeuristic(gs, m_rnd);
    }

    void mctsSearch(ElapsedCpuTimer elapsedTimer, int Rewards) {

        //1.initialise parameters
        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;
        //until stop condition met
        while(!stop){

            GameState state = rootState.copy(); //create copy of game state
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer(); //begin time
            mySingleTreeNode selected = treePolicy(state, Rewards); //decide whether to expand or not
            double delta = selected.rollOut(state); //return heuristic
            backUp(selected, delta); //return to parent node

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

    private mySingleTreeNode treePolicy(GameState state, int Rewards) {

        mySingleTreeNode cur = this;

        while (!state.isTerminal() && cur.m_depth < params.rollout_depth)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state);

            } else {
                cur = cur.uct(state, Rewards);
            }
        }

        return cur;
    }


    private mySingleTreeNode expand(GameState state) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++)
        {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {

                bestAction = i; //best action indexed by child
                bestValue = x; //random number
            }
        }

        //Roll the state
        roll(state, actions[bestAction]);

        mySingleTreeNode tn = new mySingleTreeNode(params,this,bestAction,this.m_rnd,num_actions,
                actions, fmCallsCount, rootStateHeuristic);
        children[bestAction] = tn;
        return tn;
    }

    private void roll(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                actionsAll[i] = act;
            }else {
                int actionIdx = m_rnd.nextInt(gs.nActions());
                actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }

        gs.next(actionsAll);

    }

    // function to append value to an array
    public static double[] addX(double[] arr, double x)
    {
        int n = arr.length;
        double[] newarr = new double[n + 1];
        System.arraycopy(arr, 0, newarr, 0, n);
        newarr[n] = x;

        return newarr;
    }

    // function to get mean from input array
    public static double getMean(double[] arr)
    {
        int n = arr.length;
        if (n == 0){
            return 0;
        }
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }

        return sum/arr.length;
    }

    // function to get variance from input array and mean, take square root to get standard deviation
    public static double getSTD(double[] arr, double average)
    {
        double sum = 0;
        for (double v : arr) {
            sum += Math.pow((v - average), 2);
        }

        return sum/(arr.length);
    }

        //depth-limited alpha beta pruning
        // how to call - minimax(0, depth,very low number, very higher number, true, cumulativeValue)
    private double minimax(mySingleTreeNode state, double depth , double alpha, double beta, boolean maxPlayer, double cumulativeValue){

        double decay_constant = 0.95;

        if (depth == 0){
            return cumulativeValue;
        }

        if (maxPlayer){
            double maxEva = -Double.MAX_VALUE;
            //let`s go through the children
            for (mySingleTreeNode child : this.children) {

                double childValue =  child.totValue / (child.nVisits + params.epsilon); //update child value based on number of visits and epsilon
                childValue = Utils.normalise(childValue, bounds[0], bounds[1]); //normalise child value
                double uctValue = childValue + params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon));
                cumulativeValue = uctValue*Math.pow(decay_constant,child.m_depth) + cumulativeValue;

                double Eva = minimax(child, depth -1,alpha, beta, false, cumulativeValue);
                if (Eva > maxEva){
                    maxEva = Eva; }
                if (maxEva > alpha){
                    alpha = maxEva; }
                if (alpha >= beta) {
                    break;  } //prune the tree
            }return maxEva;
        } else
        {
            double minEva = Double.MAX_VALUE; //set very high initial value
            //let`s go through the children
            for (mySingleTreeNode child : this.children)
            {
                double childValue =  child.totValue / (child.nVisits + params.epsilon); //update child value based on number of visits and epsilon
                childValue = Utils.normalise(childValue, bounds[0], bounds[1]); //normalise child value
                double uctValue = childValue + params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon)) ;
                cumulativeValue = uctValue*Math.pow(decay_constant,child.m_depth) + cumulativeValue;

                double Eva = minimax(child, depth -1, alpha, beta, true, cumulativeValue);
                if (Eva < minEva){
                    minEva = Eva; }
                if (minEva < beta) {
                    beta = minEva; }
                if (beta <= alpha) {
                    break; } //prune the tree
            }return minEva;
        }
    }

    private mySingleTreeNode uct(GameState state, int Rewards) {
        mySingleTreeNode selected_max = null;
        mySingleTreeNode selected_min = null;
        double bestValue = -Double.MAX_VALUE; //set very low initial value
        double worstValue = Double.MAX_VALUE; //set very high initial value
        boolean toPrune = false;
        double[] uctValues = { };

        for (mySingleTreeNode child : this.children)
        {

            // uncomment for desired ammendment

            double hvVal = child.totValue; //get child value
            double childValue =  hvVal / (child.nVisits + params.epsilon); //update child value based on number of visits and epsilon
            childValue = Utils.normalise(childValue, bounds[0], bounds[1]); //normalise child value
            double uctValue = childValue + params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon));
                        //PROGRESSIVE BIAS
            //uctValue = uctValue + this.rootStateHeuristic.evaluateState(state)/(1+child.nVisits);
                        //Decaying Rewards
            //double decay_constant = 0.95; //0.95, 0.9, 0.99
            //uctValue = uctValue*Math.pow(decay_constant,child.m_depth); //the further away the node from the parent, the less we trust it
                        //end of decaying rewards
            //uctValue = minimax(child, 3, -Double.MAX_VALUE, Double.MAX_VALUE, true, uctValue);

                        //this.nVisits = N(s)
                        //child.nVisits = N(s,a)
                        //Q(s,a) = childValue

                        // alternate UCT equations
            uctValues = addX(uctValues, uctValue); // function to append a value to an array
            double mean = getMean(uctValues);
            double variance = getSTD(uctValues, mean);

            double rewardsExploration = 5;
            double Vsa = variance + Math.sqrt(2 * Math.log(rewardsExploration)/ (child.nVisits + params.epsilon));
            double min_value = Math.min(Vsa, 0.25);
            double UCB_tuned = childValue + params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon))*min_value;
            //double bayes_UCT1 = mean + Math.sqrt(2*Math.log(this.nVisits+1)/(child.nVisits+params.epsilon));
            //double bayes_UCT2 = mean + Math.sqrt(2*variance*Math.log(this.nVisits+1)/ (child.nVisits + params.epsilon));


            uctValue = Utils.noise(UCB_tuned, params.epsilon, this.m_rnd.nextDouble());//break ties randomly

            if (uctValue > bestValue) {
                selected_max = child;
                bestValue = uctValue;
            }
            if (toPrune)
            {
                if (uctValue < worstValue) //attempting pruning
                {

                    selected_min = child;
                    worstValue = uctValue;
                    if (worstValue < bestValue) {
                        break;
                    }
                }
            }
        }
        if (selected_max == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " + + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        roll(state, actions[selected_max.childIdx]);

        return selected_max;
    }

    private double rollOut(GameState state)
    {
        int thisDepth = this.m_depth;

        while (!finishRollout(state,thisDepth)) {
            int action = safeRandomAction(state);
            roll(state, actions[action]);
            thisDepth++;
        }

        return rootStateHeuristic.evaluateState(state);
    }

    private int safeRandomAction(GameState state)
    {
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        while(actionsToTry.size() > 0) {

            int nAction = m_rnd.nextInt(actionsToTry.size());
            Types.ACTIONS act = actionsToTry.get(nAction);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    return nAction;

            actionsToTry.remove(nAction);
        }

        //Uh oh...
        return m_rnd.nextInt(num_actions);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean finishRollout(GameState rollerState, int depth)
    {
        if (depth >= params.rollout_depth)      //rollout end condition.
            return true;

        if (rollerState.isTerminal())               //end of game
            return true;

        return false;
    }

    private void backUp(mySingleTreeNode node, double result)
    {
        mySingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
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

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
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

    int bestAction() //originally had a private
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
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

    int mostVisitedBestAction() {
        int selected = -1;
        double bestValue_visits = -Double.MAX_VALUE;
        double bestValue_actions = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue_visits = children[i].nVisits;
                childValue_visits = Utils.noise(childValue_visits, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly

                double childValue_actions = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue_actions = Utils.noise(childValue_actions, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly


                if (childValue_visits > bestValue_visits && childValue_actions > bestValue_actions) {
                    bestValue_visits = childValue_visits;
                    bestValue_actions = childValue_actions;
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



    private boolean notFullyExpanded() {
        for (mySingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }

}
