package players.groupk;

import core.GameState;
import players.Player;

import players.optimisers.ParameterizedPlayer;
import utils.ElapsedCpuTimer;
import utils.Types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EvoMCTSPlayer extends ParameterizedPlayer {

    //Random generator.
    private Random RANDOM;

    //All actions available.
    public Types.ACTIONS[] actions;

    // Current nodes of the player:
//    public EvoNode root;
//    public EvoNode bestChild;

    // Params for this MCTS
    public EvoNodeParams params;
    private boolean done;
    private boolean init = false;

    public Types.ACTIONS[] currentSequence;

    // Constructors: ***************************************************************************************************
    public EvoMCTSPlayer(long seed, int id) {
        this(seed, id, new EvoNodeParams());
    }

    // 5 rolls stacked one after the other (or implement a shift buffer to re-use previous search) -- do in player.
    public EvoMCTSPlayer(long seed, int id, EvoNodeParams params) {
        super(seed, id, params);
        reset(seed, id);

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        actions = new Types.ACTIONS[actionsList.size()];
        int i = 0;
        for (Types.ACTIONS act : actionsList) {
            actions[i++] = act;
        }

//        root = new EvoNode(params, actions);
        done = true;

    }
    // *****************************************************************************************************************

    @Override
    public Types.ACTIONS act(GameState gs) {
        if (gs.getGameMode().equals(Types.GAME_MODE.TEAM_RADIO)) {
            int[] msg = gs.getMessage();
        }

        ElapsedCpuTimer ect = new ElapsedCpuTimer();
        ect.setMaxTimeMillis(params.num_time);

        // Create a root note; pass game state
        EvoNode rootNode;
        if(currentSequence == null){
            rootNode = new EvoNode(params, actions);
            rootNode.setGameState(gs);
            rootNode.initializeRootGenome(gs);
        } else {
            rootNode = new EvoNode(params, actions, currentSequence);
            rootNode.setGameState(gs);
        }

        // Find best action using EMCTS Algorithm
        rootNode.MCTSSearch(ect); // This call will terminate after a certain amount of time

        EvoNode bestChild = rootNode.findBestChildren();
        Types.ACTIONS[] currentSequence = bestChild.getGeneSequence();

        // debug
//        for (Types.ACTIONS a : currentSequence)
//        {
//            System.out.print(a.toString() + ", ");
//        }
//        System.out.println();

        // Turn it to Collections for better looping
        List<Types.ACTIONS[]> geneList = new ArrayList<Types.ACTIONS[]>();
        Collections.addAll(geneList, currentSequence);

        // remove as you go.
        int actionToTake;
        while (true) {
            actionToTake = Types.ACTIONS.all().indexOf(currentSequence[0]);
            geneList.remove(0);

            if (geneList.isEmpty()) {
                break;
            }
        }

        return actions[actionToTake];
    }

    @Override
    public int[] getMessage() {
        int[] message = new int[Types.MESSAGE_LENGTH];
        message[0] = 1;
        return message;
    }

    @Override
    public Player copy() {
        return new EvoMCTSPlayer(seed, playerID, params);
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);
        RANDOM = new Random(seed);

        this.params = (EvoNodeParams) getParameters();
        if (this.params == null) {
            this.params = new EvoNodeParams();
            super.setParameters(this.params);
        }
    }
}
