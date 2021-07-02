How to call the agent:

In Run.java add the following code to the case statement, X being Xth case:

case X:
    EvoNodeParams evoNodeParams = new EvoNodeParams();
    p = new EvoMCTSPlayer(seed, playerID++, evoNodeParams);
    playerStr[i-4] = "EvoMCTS";
    break;

Evolutionary Node parameters are kept inside EvoNodeParams and set to constants which are called by EvoMCTSPlayer. Initialize EvoNodeParams object and pass it as shown above.

Same approach can be used in Test.java as well:

    EvoNodeParams evoNodeParams = new EvoNodeParams();
    players.add(new EvoMCTSPlayer(seed, playerID++, evoNodeParams));