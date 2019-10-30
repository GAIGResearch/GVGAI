/**
 * Custom abstract player
 * 
 * @author Florian Kirchgessner (Alias: Number27)
 * @version customGA3
 */

package agents.Number27;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

public abstract class CustomAbstractPlayer {
	
    public abstract Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer);

    public abstract void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer);
    
    public abstract boolean switchController();
    
}
