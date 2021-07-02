package players.emcts;

import core.GameState;
import players.Player;
import utils.Types;

public class EMCTSPlayer extends Player {
    /**
     * Default constructor, to be called in subclasses (initializes player ID and random seed for this agent.
     *
     * @param seed - random seed for this player.
     * @param pId  - this player's ID.
     */
    protected EMCTSPlayer(long seed, int pId) {
        super(seed, pId);
    }

    /**
     * Function requests an action from the agent, given current game state observation.
     *
     * @param gs - current game state.
     * @return - action to play in this game state.
     */
    @Override
    public Types.ACTIONS act(GameState gs) {
        return null;
    }

    /**
     * Function that is called for requesting a message from the player
     *
     * @return int array, representing the message to be passed for the teammate
     */
    @Override
    public int[] getMessage() {
        return new int[0];
    }

    @Override
    public Player copy() {
        return null;
    }
}
