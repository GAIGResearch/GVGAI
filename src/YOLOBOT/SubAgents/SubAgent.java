package YOLOBOT.SubAgents;

import java.awt.Graphics2D;

import ontology.Types;
import tools.ElapsedCpuTimer;
import YOLOBOT.YoloState;

public abstract class SubAgent {
	public SubAgentStatus Status;
	
	public SubAgent() {
		Status = SubAgentStatus.IDLE;
	}
	
	public abstract Types.ACTIONS act(YoloState yoloState, ElapsedCpuTimer elapsedTimer);
	
	public abstract double EvaluateWeight(YoloState yoloState);

	public abstract void preRun(YoloState yoloState, ElapsedCpuTimer elapsedTimer);
	
    public void draw(Graphics2D g){
    }
}
