package agents.MaastCTS2.heuristics.events;

/**
 * Objects of this class encode knowledge obtained from observing collision events involving:
 * <br> 1. The avatar or a sprite created by the avatar, and
 * <br> 2. Another sprite of a specific type
 *
 * @author Dennis Soemers
 */
public class EventKnowledge {
	
	/** The number of times we have observed events of this type */
	private int observationCount;
	/** The sum of the changes in score observed simultaneously with observing events of this type */
	private double totalDeltaScore;
	/** The number of times that a collision event with this type was observed in a tick where our movement got blocked */
	private int blockCount;
	/** The number of times that a collision event with this type was observed in a tick where we lose */
	private int lossCount;
	/** The number of times that a collision event with this type appeared to result in the object of this type disappearing (or moving) */
	private int disappearCount;
	
	public EventKnowledge(){
		observationCount = 0;
		totalDeltaScore = 0.0;
		blockCount = 0;
		lossCount = 0;
		disappearCount = 0;
	}
	
	/**
	 * Returns the average change in score observed simultaneously with events of this type
	 * 
	 * @return
	 */
	public double getAverageDeltaScore(){
		return (totalDeltaScore / observationCount);
	}
	
	public int getBlockCount(){
		return blockCount;
	}
	
	public int getDisappearCount(){
		return disappearCount;
	}
	
	public int getLossCount(){
		return lossCount;
	}
	
	public int getObservationCount(){
		return observationCount;
	}
	
	/**
	 * Updates this knowledge with a new observation with the given total change in score
	 * 
	 * @param deltaScore
	 * @param movementBlocked
	 */
	public void updateObservation(double deltaScore, boolean movementBlocked, boolean loss, boolean disappearedObject){
		observationCount += 1;
		totalDeltaScore += deltaScore;
		
		if(movementBlocked){
			blockCount += 1;
		}
		
		if(loss){
			lossCount += 1;
		}
		
		if(disappearedObject){
			disappearCount += 1;
		}
	}

}
