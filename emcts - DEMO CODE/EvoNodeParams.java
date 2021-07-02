package players.groupk;

import players.optimisers.ParameterSet;
import utils.Pair;

import java.util.ArrayList;
import java.util.Map;

import static players.rhea.utils.Constants.CUSTOM_HEURISTIC;


public class EvoNodeParams implements ParameterSet {
    // Final Variables:
    public static final int MAX_ROLLOUTS = 10;  // how many rollouts do we go.
    public static final int BRANCH_FACTOR = 25;      // test this much child mutations per each parent.
    public final int STOP_TIME = 0;
    public final int STOP_ITERATIONS = 1;
    public final int STOP_FMCALLS = 2;

    // Budget settings
    public int stop_type = STOP_TIME;
    public int num_iterations = 200;
    public int num_fmcalls = 2000;

    // Parameters: Taken from MCTSParams
    public double K = Math.sqrt(2);
    public int heuristic_method = CUSTOM_HEURISTIC;
    public double epsilon = 1e-6;

    public int num_time = 40;

    // Not necessary to fill them up.
    @Override
    public void setParameterValue(String param, Object value) {}

    @Override
    public Object getParameterValue(String root) {
        return null;
    }

    @Override
    public ArrayList<String> getParameters() {
        return null;
    }

    @Override
    public Map<String, Object[]> getParameterValues() {
        return null;
    }

    @Override
    public Pair<String, ArrayList<Object>> getParameterParent(String parameter) {
        return null;
    }

    @Override
    public Map<Object, ArrayList<String>> getParameterChildren(String root) {
        return null;
    }

    @Override
    public Map<String, String[]> constantNames() {
        return null;
    }
}
