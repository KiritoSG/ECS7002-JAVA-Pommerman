Pommerman-MCTS
Modifications and customised MCTS Agent for Java Based Pommerman Implementation

To run agent call instance of new MCTS in following manner:

in switch statement Pommerman run file include:

case 6: 
MCTSParams mymctsParams = new MCTSParams(); 
mymctsParams.stop_type = mymctsParams.STOP_ITERATIONS; 
mymctsParams.num_iterations = 200; 
mymctsParams.rollout_depth = 12;
mymctsParams.heuristic_method = mymctsParams.CUSTOM_HEURISTIC;
p = new myMCTSPlayer(seed, playerID++, mymctsParams);
playerStr[i-4] = "customMCTS";
break;



Constructor to call player: 

players.add(new myMCTSPlayer(seed, playerID++, new MCTSParams()));