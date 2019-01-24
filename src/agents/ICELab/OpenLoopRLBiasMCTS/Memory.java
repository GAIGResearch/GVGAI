package agents.ICELab.OpenLoopRLBiasMCTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.WINNER;
import tools.StatSummary;
import tools.Vector2d;
import agents.ICELab.GameInfo;

public class Memory
{
    public HashMap<Integer, HashMap<Integer,MemoryItem>> memories;
    HashMap<Integer, Integer> distances;
    HashMap<Integer, Integer> prevDistances;

    // Record the matching of portal entry type and portal exit type
    // portal entry type is Key and the portal exit type is the Value
    // added by Jerry
    public HashMap<Integer, Integer> portalMemories;

    public double gain;
    public double discoverScore;

    public boolean[][] contraryActions;
    public boolean[] moveActions;

	// used for updating memoryItems
    final double MEAN_SCORE_THRESHOLD = 0.3;
	final double PERC_WINS_THRESHOLD  = 0.1;
	final double PERC_LOSES_THRESHOLD = 0.01;

	private static final Logger logger = Logger.getLogger("ICELab");

    public Memory(int avatar_default_type)
    {
        memories = new HashMap<Integer, HashMap<Integer,MemoryItem>>();
        memories.put(avatar_default_type, new HashMap<Integer, MemoryItem>());
        portalMemories = new HashMap<Integer, Integer>();
    }

    //Add new avatar itype
    public void AddAvatarType(int avatar_type) {
    	if(memories.get(avatar_type)==null){
    		memories.put(avatar_type, new HashMap<Integer, MemoryItem>());
    	}
	}

    public boolean noEffect(StateObservation st, int action, StateObservation stNext)
    {
        if(!moveActions[action])
            return false;

        if(compareVectors(st.getAvatarPosition(), stNext.getAvatarPosition()))
            return true;

        return false;
    }

    public void manageGameEnd(StateObservation prev, StateObservation next)
    {
    		double scorePrev = prev.getGameScore();
        double scoreNext = next.getGameScore();

        double scoreDiff = scoreNext - scorePrev;
        Event ev = retrieveLastUniqueEvent(next); //This is LIKELY the event that caused the game end or score change.
        if(ev != null && !prev.isGameOver()){
            manageEvent(ev, scoreDiff, next.getGameWinner(), true, next, prev.getAvatarType());
        }
    }

    public Event retrieveLastUniqueEvent(StateObservation stObs)
    {
        Event event = null;

        TreeSet<Event> events = stObs.getHistoricEventsHistory();
        if(events != null && events.size()>0)
        {
            Iterator<Event> it = events.descendingSet().iterator();

            //Last event and its gameStep.
            event = it.next();
            int gameStep = event.gameStep;

            while(it.hasNext())
            {
                Event e = it.next();

                if((gameStep == e.gameStep))
                    return null;  //A second event with the same gameStep. Not unique, return null.
                else
                    return event; //Another event with a different gameStep. As it is ordered, is unique, return it.
            }
        }

        //Either null (no events), or the last and only (unique) event in the history.
        return event;
    }

    HashMap<Integer,Integer> extractDistances(StateObservation so){
    		HashMap<Integer,Integer> distances = new HashMap<Integer,Integer>();

    		ArrayList<ArrayList<Observation>[]> sprites = new ArrayList<ArrayList<Observation>[]>();

		sprites.add(so.getResourcesPositions(so.getAvatarPosition()));
		sprites.add(so.getNPCPositions(so.getAvatarPosition()));
		sprites.add(so.getImmovablePositions(so.getAvatarPosition()));
		sprites.add(so.getMovablePositions(so.getAvatarPosition()));
		sprites.add(so.getPortalsPositions(so.getAvatarPosition()));

		Pathfinder pf = new Pathfinder();
		pf.updateMap(so);

		for (ArrayList<Observation>[] type : sprites){
			if (type != null)
				for (int i = 0; i < type.length; i++){
					if (type[i].size() > 0){
						int itemType = type[i].get(0).itype;
						int distance = pf.getAStarDistance(so, Utils.toTileCoord(type[i].get(0).position));
						//System.out.println(itemType + " dist.:" + distance );
						if (distance != -1)
							distances.put(itemType, distance);
					}
				}
		}
    		return distances;
    }

	public int getDistanceChanges(Node node)
    {
		int gain = 0;
		this.distances = extractDistances(node.state);
		this.prevDistances = extractDistances(node.prev.state);
		int avatarType = (node.state.isGameOver())? GameInfo.avatarType:node.state.getAvatarType();
		for (Integer type : distances.keySet()){
			if (!memories.get(avatarType).containsKey(type) ||
					memories.get(avatarType).get(type).beneficial){
				if (prevDistances.containsKey(type))
				gain = prevDistances.get(type) - distances.get(type);
			}
		}
        return gain;
    }

    public void updateMemoryItemsHostility(){
    	for (int key : memories.keySet()){
	    	Iterator<Map.Entry<Integer, MemoryItem>> itEntries = memories.get(key).entrySet().iterator();
	    	while(itEntries.hasNext())
	    		{
	    		Map.Entry<Integer, MemoryItem> entry = itEntries.next();
	    		MemoryItem mem = entry.getValue();
	    		//System.out.print("type " + entry.getKey());
	    		if (!portalMemories.containsKey(entry.getKey())) // if it is portal entry, do not set hostility
	    			mem.updateHostility();
	        }
	    	}
    }

    public class MemoryItem
	{
	    double FIRST_KNOWLEDGE = 10;
	    StatSummary standaloneSS;
	    StatSummary multipleSS;
	    StatSummary actionSS;
	    int timestamp;
	    boolean traversable;
	    boolean killable;
	    boolean hostile;
	    boolean beneficial;
	    boolean pushable;

	    int intraversableCount = 0;
	    final int traversableThreshold = 1;

	    double nWins;
	    double nLoses;
	    double nNone;

	    public MemoryItem()
	    {
	        standaloneSS = new StatSummary();
	        multipleSS = new StatSummary();
	        actionSS = new StatSummary();
	        traversable = true;
	        killable = hostile = beneficial = false;
	        nWins = nLoses = nNone = 0;
	        pushable=false;
	    }

	    public void updateHostility(){
	    	if (getTotalOcc() > 0){

	    		if(getOverallMean() > MEAN_SCORE_THRESHOLD || getPercWins() > PERC_WINS_THRESHOLD) {
	    			//logger.info(" mean: " + getOverallMean() + " PercWins: " + getPercWins());
	    			beneficial = true;
	    			hostile = false;
	    		} else if (getPercLoses() > PERC_LOSES_THRESHOLD || getOverallMean() < -MEAN_SCORE_THRESHOLD) {
	    			//logger.info(" mean: " + getOverallMean() + " PercLoses: " + getPercLoses());
	    			beneficial = false;
	    			hostile = true;
	    		}
	    		else {
	    			beneficial = false;
	    			hostile = false;
	    			//logger.info(" indeterminate");
	    		}
	    	}
	    	else{
	    		//logger.info(" no record");
	    	}
	    }

	    public int getTotalOcc()
	    {
	        return standaloneSS.n() + multipleSS.n() + actionSS.n();
	    }

	    public boolean isBeneficial(){
	    	return beneficial;
	    }

	    public boolean isHostile(){
	    	return hostile;
	    }

	    public double getTotalMeanScore()
	    {
	        double totMean = 0.0;
	        if(standaloneSS.n()>0) totMean+=standaloneSS.mean();
	        if(multipleSS.n()>0) totMean+=multipleSS.mean();
	        if(actionSS.n()>0) totMean+=actionSS.mean();
	        return totMean;
	    }

	    // added by Jerry
	    public double getOverallMean()
	    {
	    	double total = 0.0;
	    	int n = 0;
	        if(standaloneSS.n()>0) {
	        	n += standaloneSS.n();
	        	total += standaloneSS.sum();
	        }
	        if(multipleSS.n()>0) {
	        	n += multipleSS.n();
	        	total += multipleSS.sum();
	        }
	        return total / n;
	    }

	    public double getCollScore()
	    {
	        double totMean = 0.0;
	        if(standaloneSS.n()>0) totMean+=standaloneSS.mean();
	        if(multipleSS.n()>0) totMean+=multipleSS.mean();
	        return totMean;
	    }

	    public double getPercWins()
	    {
	        if(nWins == 0) return 0;
	        return nWins / (nWins + nLoses + nNone);
	    }

	    public double getPercLoses()
	    {
	        if(nLoses == 0) return 0;
	        return nLoses / (nWins + nLoses + nNone);
	    }

	    public double getActScore()
	    {
	        double totMean = 0.0;
	        if(actionSS.n()>0) totMean+=actionSS.mean();
	        return totMean;
	    }

	    public void addOcc(double scoreChange, Types.WINNER winner, boolean standalone, int id, boolean fromAvatar, boolean killable, int timestamp){
	    	if (fromAvatar && killable){
	    		this.killable = killable;
	    	}
	    	addOcc(scoreChange, winner, standalone, id, fromAvatar, timestamp);
	    }

	    public void addOcc(double scoreChange, Types.WINNER winner, boolean standalone, int id, boolean fromAvatar, int timestamp)
	    {

	        if(fromAvatar)
	        {
	            if(standalone)
	            {
	                actionSS.add(scoreChange);
	                //logger.info(scoreChange);
	            }else return;
	        }else{
	            if(standalone)
	            {
	                standaloneSS.add(scoreChange);
	            }else
	                multipleSS.add(scoreChange);
	        }
	        this.timestamp = timestamp;

	        if(winner == Types.WINNER.PLAYER_LOSES)
	            nLoses++;
	        else if(winner == Types.WINNER.PLAYER_WINS)
	        {
	            //Config.USE_ASTAR = true;
	            nWins++;
	        }
	        else nNone++;

	    }

	    public void setTraversable(boolean trav)
	    {
	        this.traversable = trav;
	    }

	    public boolean isTraversable()
	    {
	        return traversable;
	    }

	    public void setKillable(boolean kill)
	    {
	        this.killable = kill;
	    }

	    public boolean isKillable()
	    {
	        return killable;
	    }

	    private double gain(double pre, double post)
	    {
	        if(pre == 0)
	            return  post * FIRST_KNOWLEDGE;
	        return (post / pre) - 1;
	    }

	    public double computeGain()
	    {
	        return gain(0,standaloneSS.n()) + gain(0,multipleSS.n()) + gain(0,actionSS.n());
	    }

	    public double computeGain(MemoryItem pastMemory)
	    {
	        double gain = 0;
	        //System.out.println("past: " + pastMemory.getTotalOcc() + " now: " + getTotalOcc());
	        gain += gain(pastMemory.standaloneSS.n(), standaloneSS.n());
	        gain += gain(pastMemory.multipleSS.n(), multipleSS.n());
	        gain += gain(pastMemory.actionSS.n(), actionSS.n());

	        return gain;
	    }

	    public void reset()
	    {
	        timestamp = 0;
	        standaloneSS = new StatSummary();
	        multipleSS = new StatSummary();
	        actionSS = new StatSummary();
		    traversable = true;
		    killable = false;
		    hostile = false;
		    beneficial = false;
	    }

	    public MemoryItem copy()
	    {
	        MemoryItem mm = new MemoryItem();
	        mm.timestamp = this.timestamp;
	        mm.standaloneSS = this.standaloneSS.copy();
	        mm.multipleSS = this.multipleSS.copy();
	        mm.actionSS = this.actionSS.copy();

	        mm.timestamp = this.timestamp;
	        mm.traversable = this.traversable;
	        mm.nWins = this.nWins;
	        mm.nLoses = this.nLoses;
	        mm.nNone = this.nNone;

	        return mm;
	    }

	    public String toString()
	    {
	        return "[alone oc: " + standaloneSS.n() + ", mean: " + standaloneSS.mean() + "]" +
	               "[mult oc " + multipleSS.n() + ", mean: " + multipleSS.mean() + "]" +
	               "[actn oc " + actionSS.n() + ", mean: " + actionSS.mean() + "]";
	    }
	}

	public double getPercWins(int key, int avatarType)
    {
        if(memories.containsKey(avatarType) && memories.get(avatarType).containsKey(key))
            return memories.get(avatarType).get(key).getPercWins();
        return 0.0;
    }


    public double getPercLoses(int key, int avatarType)
    {
        if(memories.containsKey(avatarType) && memories.get(avatarType).containsKey(key))
            return memories.get(avatarType).get(key).getPercLoses();
        return 0.0;
    }

    public double getTotalMeanScore(int key, int avatarType)
    {
        if(memories.containsKey(avatarType) && memories.get(avatarType).containsKey(key))
            return memories.get(avatarType).get(key).getTotalMeanScore();
        return 0.0;
    }


    public double getDiscoverScore()
    {
        return discoverScore;
    }

    public void addInformation(StateObservation soPrev, StateObservation soNext, Types.ACTIONS action)
    {
    		double gainedScore = getGainedScore(soNext, soPrev.getGameScore());
        int numNewEvents = soNext.getHistoricEventsHistory().size() - soPrev.getHistoricEventsHistory().size();
        int totalNewGameEvents = numNewEvents;
        int avatarType = (soPrev.isGameOver())? GameInfo.avatarType : soPrev.getAvatarType();
        if (!memories.containsKey(avatarType))
        		memories.put(avatarType, new HashMap<Integer, MemoryItem>());

        if (!soNext.isGameOver() && !memories.containsKey(soNext.getAvatarType()))
    		memories.put(soNext.getAvatarType(), new HashMap<Integer, MemoryItem>());

        if(numNewEvents == 0){
        		int prevX = (int) soPrev.getAvatarPosition().x/soPrev.getBlockSize();
            int prevY = (int) soPrev.getAvatarPosition().y/soPrev.getBlockSize();
            int nextX = (int) soNext.getAvatarPosition().x/soNext.getBlockSize();
            int nextY = (int) soNext.getAvatarPosition().y/soNext.getBlockSize();
            if (prevX != nextX || prevY != nextY)
            	handleEventlessCollisions(soNext, avatarType);
        }

        int manEvents = 0;//manualCollisions(features, soNext, gainedScore, numNewEvents);
        //ArrayList<Observation> collidedObv = soNext.getObservationGrid()[(int) soNext.getAvatarPosition().x/soNext.getBlockSize()][(int) soNext.getAvatarPosition().x/soNext.getBlockSize()];
        //if(numNewEvents != 1)
         //   return;
        Event ev;
        Iterator<Event> itEvent = soNext.getHistoricEventsHistory().descendingSet().iterator();
        while(numNewEvents > 0 && itEvent.hasNext())
        {
            ev = itEvent.next();
            if(ev != null && action != Types.ACTIONS.ACTION_USE && action != Types.ACTIONS.ACTION_NIL)
            {
            	if(compareVectors(soNext.getAvatarPosition(), soPrev.getAvatarPosition()))
                {
            		Utils.logger.finest("MEMORY | Colliding with " + ev.passiveTypeId + " didn't let me move " + action + " at " + ev.gameStep);
                    manageTraverse(ev, false, avatarType);
                } else
                		manageTraverse(ev, true, avatarType);

                if(enteredPortal(soPrev, ev.passiveTypeId)){
                		managePortal(soNext, ev.passiveTypeId, avatarType);
                }
            }
            manageEvent(ev, gainedScore, soNext.getGameWinner(), (manEvents+totalNewGameEvents) == 1, soNext, avatarType);
            numNewEvents--;
        }
        this.updateMemoryItemsHostility();
    }

    // added by Jerry
    // handle collisions that do not generate any event
    private void handleEventlessCollisions(StateObservation so, int avatarType) {
    	int x = (int) so.getAvatarPosition().x/so.getBlockSize();
    	int y = (int) so.getAvatarPosition().y/so.getBlockSize();
    	if(!Utils.isOnMap(so, x, y))
    			return;
    	ArrayList<Observation> collidedObv = so.getObservationGrid()[x][y];
    	if (collidedObv != null && collidedObv.size() != 0)
    		for (Observation o : collidedObv){
    			if (o.itype != avatarType){
    				Event ev = new Event(so.getGameTick(), false, 1, o.itype, 0, 0, so.getAvatarPosition());
    				Utils.logger.info("handle eventless collision type " + o.itype + " at gameTick " + so.getGameTick() +
    									   " at (" + so.getAvatarPosition().x/so.getBlockSize() + ", " + so.getAvatarPosition().y/so.getBlockSize() + ")");
    				manageEvent(ev, 0, so.getGameWinner(), true, so, avatarType);
    			}
    		}
	}

	private void managePortal(StateObservation soNext, int passiveTypeId, int avatarType) {
		// check the sprite types at avatar's position
		int currentX = (int) soNext.getAvatarPosition().x/soNext.getBlockSize();
    	int currentY = (int) soNext.getAvatarPosition().y/soNext.getBlockSize();

    	ArrayList<Observation> avatarPositionSprites = soNext.getObservationGrid()[currentX][currentY];
    	ArrayList<Observation>[] immovables = soNext.getImmovablePositions();

    	for (ArrayList<Observation> immovable : immovables)
    		for (Observation obs : avatarPositionSprites)
    			if(immovable.size() > 0 && immovable.get(0).itype == obs.itype){
    				// this type is probably the portal exit type
    				portalMemories.put(passiveTypeId, obs.itype);
    				//logger.info("Portal type " + passiveTypeId + " is linked to " + obs.itype);
    			}
	}

	private boolean enteredPortal(StateObservation soPrev, int passiveTypeId) {
		// check if the event passive type is a portal entry
		ArrayList<Observation>[] portals = soPrev.getPortalsPositions();
		if (portals != null && portals.length != 0)
			for (ArrayList<Observation> obs : portals)
				if (obs != null && obs.size() != 0 && obs.get(0).itype == passiveTypeId)
					return true;
		return false;
	}

	private double getGainedScore(StateObservation soNext, double prevScore) {

        boolean gameLost = soNext.isGameOver() && soNext.getGameWinner() == Types.WINNER.PLAYER_LOSES;
        double gainedScore = 0;
        if(gameLost)
            gainedScore = -100;
        else
            gainedScore = soNext.getGameScore() - prevScore;
        return gainedScore;
    }

    public boolean isTraversable(int typeId, int avatarType)
    {
        if(!memories.get(avatarType).containsKey(typeId))
        {
            return true;
        }
        return memories.get(avatarType).get(typeId).isTraversable();
    }

    public void manageTraverse(Event ev, boolean traversable, int avatarType)
    {
    		MemoryItem mem;
    		if(!memories.get(avatarType).containsKey(ev.passiveTypeId))
        {
            mem = new MemoryItem();
            Utils.logger.info("Add new type " + ev.passiveTypeId + " traversable: " + traversable);
            (memories.get(avatarType)).put(ev.passiveTypeId, mem);
        }else
            mem = memories.get(avatarType).get(ev.passiveTypeId);

        if (!traversable){
        	mem.intraversableCount++;
        } else
        	mem.intraversableCount = (mem.intraversableCount == 0)? 0:mem.intraversableCount-1;
        if ( mem.intraversableCount > mem.traversableThreshold){
        	Utils.logger.finer("MEMORY | Type " + ev.passiveTypeId + " intraversableCount: " + mem.intraversableCount);
        	Utils.logger.fine("MEMORY | set " + ev.passiveTypeId + " traversable: " + traversable);
        	mem.setTraversable(false);
        }
        else
        	mem.setTraversable(true);
    }

    public void manageEvent(Event ev, double gainedScore, Types.WINNER winner, boolean standalone, StateObservation so, int avatarType)
    {
        MemoryItem mem;
        if(!memories.get(avatarType).containsKey(ev.passiveTypeId))
        {
            mem = new MemoryItem();
            //logger.info("Add new type " + ev.passiveTypeId + " Score: " + gainedScore + " Winner: " + winner);
            memories.get(avatarType).put(ev.passiveTypeId, mem);
        }else
            mem = memories.get(avatarType).get(ev.passiveTypeId);

     // check if the sprite is killable by ACTION_USE
    	if (ev.fromAvatar){
    		int eventX = (int) ev.position.x/GameInfo.blocksize;
    		int eventY = (int) ev.position.y/GameInfo.blocksize;
    		 if(Utils.isOnMap(so, eventX, eventY) &&
    			!so.getObservationGrid()[eventX][eventY].contains(ev.passiveTypeId))
    	            mem.setKillable(true);
    	}

    	logger.finest("MEMORY | Add new item for type " + ev.passiveTypeId + " Score: " + gainedScore + " Winner: " + winner + " GameStep: " + ev.gameStep);
        mem.addOcc(gainedScore, winner, standalone, ev.passiveTypeId, ev.fromAvatar, ev.gameStep);
    }

    //add by Jerry
    public void manageEvent(Event ev, double gainedScore, Types.WINNER winner, boolean standalone, boolean killable, int avatarType)
    {
        MemoryItem mem;
        if(!memories.get(avatarType).containsKey(ev.passiveTypeId))
        {
            mem = new MemoryItem();
            logger.finer("MEMORY | Add new type " + ev.passiveTypeId + " Score: " + gainedScore + " Winner: " + winner);
            (memories.get(avatarType)).put(ev.passiveTypeId, mem);
        }else
            mem = memories.get(avatarType).get(ev.passiveTypeId);

        mem.addOcc(gainedScore, winner, standalone, ev.passiveTypeId, ev.fromAvatar, killable, ev.gameStep);
    }

    // added by Jerry
	public void manageCollision(int itype, double scoreDiff, WINNER gameWinner, boolean standalone, int gameStep, int avatarType) {
		MemoryItem mem;
        if(!memories.get(avatarType).containsKey(itype))
        {
            mem = new MemoryItem();
            (memories.get(avatarType)).put(itype, mem);
        }else
            mem = memories.get(avatarType).get(itype);

        mem.addOcc(scoreDiff, gameWinner, standalone, itype, true, gameStep);
	}

    public void report()
    {
    	Iterator<Map.Entry<Integer, HashMap<Integer, MemoryItem>>> avatarTypes = memories.entrySet().iterator();
    	while(avatarTypes.hasNext()){
    		int avatarType = avatarTypes.next().getKey();
    		String report = "";
    		report += "\nMEMORY | Avatar Type: " + avatarType + "----------------------------------\n";
	        Iterator<Map.Entry<Integer, MemoryItem>> itEntries = memories.get(avatarType).entrySet().iterator();
	        while(itEntries.hasNext())
	        {
	            Map.Entry<Integer, MemoryItem> entry = itEntries.next();

	            MemoryItem mem = entry.getValue();
	            Integer key = entry.getKey();
	            report += "Entry of type " + key + ", with timestamp " + mem.timestamp + ": \n";
	            report += "\t * makes me win at " + mem.getPercWins()*100.0 + "%, lose at "
	                    + mem.getPercLoses()*100.0 + "%.\n";
	            report += "\t * is " + (mem.traversable ? "traversable" : "not traversable") + " (intraversableCount: " + mem.intraversableCount + ")\n";
	            report += "\t * is " + (mem.killable ? "killable\n" : "not killable\n");
	            if (mem.isBeneficial())
	            	report += "\t * is beneficial\n";
	            else if (mem.isHostile())
	            	report += "\t * is hostile\n";
	            else
	            	report += "\t * is neutral\n";
	            if (portalMemories.containsKey(key))
	            	report += "\t * is portal entry. Connected to type " + portalMemories.get(key) + "\n";
	            if (portalMemories.containsValue(key))
	            	report += "\t * is portal exit.\n";
	            report += "\t * is been seen " + mem.getTotalOcc() + "\n";
	            report += "\t * produces an average score of " + mem.getCollScore() + " on collision.\n";
	            report += "\t * produces an average score of " + mem.getActScore() + " with from-avatar sprites.\n";
	        }
	        Utils.logger.info(report);
    	}
    }

    boolean compareVectors(Vector2d a, Vector2d b){
    	return (Math.abs(a.x - b.x) < 0.01) && (Math.abs(a.y - b.y) < 0.01);
    }

}
