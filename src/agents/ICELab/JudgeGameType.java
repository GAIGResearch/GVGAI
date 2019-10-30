package agents.ICELab;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class JudgeGameType {

	public boolean isDetermin = false;
	public int block_size = 0;
	//true:DeterministicGame
	//false:NonDeterministicGame
	public JudgeGameType(StateObservation so){
		block_size = so.getBlockSize();
		isDetermin = UseHashCode(so);
		
		//isDetermin = (SomeStepActionNIL(so)&&checkIfGameIsDeterministic(so));
	}
	
	public boolean SomeStepActionNIL(StateObservation so){
		StateObservation initial_so;
		so.advance(ACTIONS.ACTION_NIL);
		initial_so=so.copy();
		int judge_step=30;
		int a=0;
		outside: for(a=0;a<judge_step;a++) {
			so.advance(ACTIONS.ACTION_NIL);
			for(int j = 0; j < so.getObservationGrid()[0].length; j++){
				for(int i = 0; i < so.getObservationGrid().length; i++){
					if(!so.getObservationGrid()[i][j].equals(initial_so.getObservationGrid()[i][j])){
						break outside;
					}
				}

			}
		}
		if(a==judge_step){
			return true;
			
		}else {
			return false;
		}
	}
	
	public boolean UseHashCode(StateObservation stateObs){
		StateObservation so = stateObs.copy();
		long initialHash;
		so.advance(ACTIONS.ACTION_NIL);
		initialHash = GenerateHashCode(so);
		int judge_step=30;
		int a=0;
		outside: for(a=0;a<judge_step;a++) {
			so.advance(ACTIONS.ACTION_NIL);
			if(initialHash!=GenerateHashCode(so)){
				break outside;	
			}
		}
		if(a==judge_step){
			return true;
			
		}else {
			return false;
		}
	}
	


	public boolean GetisDetermin(){
		return this.isDetermin;
	}
	
	public long GenerateHashCode(StateObservation so)
	{
		long result = 17;
		long prime = 31;
		ArrayList<Observation>[][] list;
		Vector2d orient;
		orient = so.getAvatarOrientation();
		
		result = result * prime + so.getAvatarType();
		result = result * prime + (int)(so.getAvatarPosition().x/block_size);		
		result = result * prime + (int)(so.getAvatarPosition().x/block_size);
		result = result * prime + (int)((orient.x * 2) + orient.y);

		list = so.getObservationGrid();
		for (int i = 0; i < list.length; i++)
		{
			for (int j = 0; j < list[i].length; j++)
			{
				result = result * prime + (i * list[i].length + j);
				for (Observation obj : list[i][j])
				{
					if (obj.category != Types.TYPE_AVATAR)
					{
						result = result * prime + obj.obsID;
						result = result * prime + obj.itype;
					}
				}
			}
		}
		return result;
	}
	

}
