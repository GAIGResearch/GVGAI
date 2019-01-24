package agents.SJA862;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.Random;
public class MinMaxPlayer
{
	public MinMaxNode root;
    public Random r;
	public double greatestDistance = 0;
	public Vector2d originalPos = null;
	
    public MinMaxPlayer(Random r)
    {
        this.r = r;
    }
	
	public int getAction (StateObservation state, ElapsedCpuTimer elapsedTimer) {
		root = new MinMaxNode (state, null, r, null);
		root.greatestDistance = greatestDistance;
		
		int action = root.search(elapsedTimer, originalPos);
		greatestDistance = root.greatestDistance;
		originalPos = root.returnPos();
		return action;
	}
}