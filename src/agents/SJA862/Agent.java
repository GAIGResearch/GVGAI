package agents.SJA862;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Random;

public class Agent extends AbstractPlayer {

    public static int NUM_ACTIONS;
    public static Types.ACTIONS[] actions;
	private MinMaxPlayer minmaxplayer;
	
	public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }
        NUM_ACTIONS = actions.length;
        minmaxplayer = new MinMaxPlayer(new Random());
    }
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		int action = minmaxplayer.getAction(stateObs, elapsedTimer);
		return actions[action];
    }
}
	
	