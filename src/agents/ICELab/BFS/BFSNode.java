package agents.ICELab.BFS;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ontology.Types;
import ontology.Types.ACTIONS;
import tools.Vector2d;
import core.game.Observation;
import core.game.StateObservation;

public class BFSNode
{
	public double value;
	public double rawvalue;
	public LinkedList<ACTIONS> actions;
	public StateObservation so;
	public long hashcode;
	public long[] code;
	public int deepth;
	Random rand;
	public static double epsilon = 1.0e-2;

	public BFSNode parent;


	public BFSNode()
	{
		code = new long[2];
		Reset();
	}

	public BFSNode(StateObservation so, ACTIONS action, BFSNode parent, Random rand)
	{
		actions = new LinkedList<ACTIONS>();
		this.rand = rand;
		Init(so, action, parent, rand);
	}

	public void Init(StateObservation so, ACTIONS action, BFSNode parent, Random rand)
	{
		this.so = so;
		this.parent = parent;

		if (parent != null)
		{
			actions.addAll(parent.actions);
		}
		if (action != null)
		{
			actions.add(action);
		}
		value = so.getGameScore() + rand.nextDouble();// * epsilon ;
		rawvalue = so.getGameScore();
//		value += PenaltyMovableConear(so);
		if (parent != null)
		{
			deepth = parent.deepth + 1;
		}
		code = new long[2];
	}

	public void Reset()
	{
		value = 0;
		rawvalue = 0;
		actions.clear();
		so = null;
		hashcode = 0;
		deepth = 0;
	}

	public void HashMe()
	{
		//hashcode = GenerateHashCode();
		GenerateHashCodeEX();
	}

	public void GenerateHashCodeEX()
	{

		int count = 0;
		long prime = 7;
		ArrayList<Observation>[][] list;
		Vector2d pos;
		double x,y;
		Vector2d orient;
		pos = so.getAvatarPosition();

		x = pos.x / (double)so.getBlockSize();
		y = pos.y / (double)so.getBlockSize();

		orient = so.getAvatarOrientation();

		code[count] = code[count] * prime + so.getAvatarType();
		count = (count + 1) % code.length;
		code[count] = code[count] * prime + Double.doubleToLongBits(x);
		count = (count + 1) % code.length;
		code[count] = code[count] * prime + Double.doubleToLongBits(y);
		count = (count + 1) % code.length;
		code[count] = code[count] * prime + Double.doubleToLongBits(orient.x);
		count = (count + 1) % code.length;
		code[count] = code[count] * prime + Double.doubleToLongBits(orient.y);
		count = (count + 1) % code.length;

		list = so.getObservationGrid();
		for (int i = 0; i < list.length; i++)
		{
			for (int j = 0; j < list[i].length; j++)
			{
				for (Observation obj : list[i][j])
				{
					code[count] = code[count] * prime + i;
					count = (count + 1) % code.length;
					code[count] = code[count] * prime + j;
					count = (count + 1) % code.length;
					if (obj.category != Types.TYPE_AVATAR)
					{
						code[count] = code[count] * prime + obj.obsID;
						count = (count + 1) % code.length;
						code[count] = code[count] * prime + obj.itype;
						count = (count + 1) % code.length;
					}
				}
			}
		}

		HashMap<Integer, Integer> res;
		res = so.getAvatarResources();
		for (int key : res.keySet())
		{
			code[count] = code[count] * prime + key;
			count = (count + 1) % code.length;
			code[count] = code[count] * prime + res.get(key);
			count = (count + 1) % code.length;
		}

		code[0] = code[0] * prime + code[1];
		code[1] = code[1] * prime + code[0];
		//hash_string = ""+avator_code + "" + static_code + "" + resource_code + "" + portal_code + "" + movable_code;

	}

	public long GenerateHashCode()
	{
		long result = 11;
		long prime = 7;
		ArrayList<Observation>[][] list;
		Vector2d pos;
		double x,y;
		Vector2d orient;
		pos = so.getAvatarPosition();

		x = pos.x / (double)so.getBlockSize();
		y = pos.y / (double)so.getBlockSize();

		orient = so.getAvatarOrientation();
		result = result * prime + so.getAvatarType();
		result = result * prime + Double.doubleToLongBits(x);
		result = result * prime + Double.doubleToLongBits(y);
		result = result * prime + Double.doubleToLongBits(orient.x);
		result = result * prime + Double.doubleToLongBits(orient.y);

		list = so.getObservationGrid();
		for (int i = 0; i < list.length; i++)
		{
			result = result*prime + i;
			for (int j = 0; j < list[i].length; j++)
			{
				result = result * prime + j;
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

		HashMap<Integer, Integer> res;
		res = so.getAvatarResources();
		for (int key : res.keySet())
		{
			result = result * prime + key;
			result = result * prime + res.get(key);
		}

		return result;
	}

	public boolean CanGuess(ACTIONS act)
	{
		int x,y;
		Vector2d pos = so.getAvatarPosition();
		ArrayList<Observation>[][] map;
		x = (int)(pos.x / (double)so.getBlockSize());
		y = (int)(pos.y / (double)so.getBlockSize());
		map = so.getObservationGrid();

		switch (act) {
		case ACTION_LEFT:
		{
			x -= 1;
			break;
		}
		case ACTION_RIGHT:
		{
			x += 1;
			break;
		}
		case ACTION_UP:
		{
			y -= 1;
			break;
		}
		case ACTION_DOWN:
		{
			y += 1;
			break;
		}
		default:
			break;
		}

		if (x < 0 || x >= map.length || y < 0 || y >= map[0].length)
		{
			return false;
		}

		if (so.getObservationGrid()[x][y].size() > 0)
		{
			return false;
		}
		else {
			return true;
		}
	}

	public String GuessHashStr(ACTIONS act)
	{

		long avator_code = 11;
		long static_code = 13;
		long resource_code = 19;
		long portal_code = 17;
		long movable_code = 23;

		long prime = 7;
		ArrayList<Observation>[][] list;
		Vector2d pos;
		double x,y;
		Vector2d orient;
		pos = so.getAvatarPosition();
		list = so.getObservationGrid();
		x = pos.x / (double)so.getBlockSize();
		y = pos.y / (double)so.getBlockSize();

		switch (act) {
		case ACTION_LEFT:
		{
			x -= 1.0;
			break;
		}
		case ACTION_RIGHT:
		{
			x += 1.0;
			break;
		}
		case ACTION_UP:
		{
			y -= 1.0;
			break;
		}
		case ACTION_DOWN:
		{
			y += 1.0;
			break;
		}
		default:
			break;
		}

		if (x < 0)
		{
			x = 0;
		}
		else if (x > list.length)
		{
			x = list.length;
		}
		if (y < 0)
		{
			y = 0;
		}
		else if (y > list[0].length)
		{
			y = list[0].length;
		}

		orient = so.getAvatarOrientation();
		avator_code = avator_code * prime + so.getAvatarType();
		avator_code = avator_code * prime + Double.doubleToLongBits(x);
		avator_code = avator_code * prime + Double.doubleToLongBits(y);
		avator_code = avator_code * prime + Double.doubleToLongBits(orient.x);
		avator_code = avator_code * prime + Double.doubleToLongBits(orient.y);


		for (int i = 0; i < list.length; i++)
		{
			for (int j = 0; j < list[i].length; j++)
			{
				for (Observation obj : list[i][j])
				{
					switch (obj.category)
					{
					case Types.TYPE_MOVABLE:
					{
						movable_code = movable_code * prime + i;
						movable_code = movable_code * prime + j;
						movable_code = movable_code * prime + obj.obsID;
						movable_code = movable_code * prime + obj.itype;
						break;
					}
					case Types.TYPE_PORTAL:
					{
						portal_code = portal_code * prime + i;
						portal_code = portal_code * prime + j;
						portal_code = portal_code * prime + obj.obsID;
						portal_code = portal_code * prime + obj.itype;
						break;
					}
					case Types.TYPE_STATIC:
					{
						static_code = static_code * prime + i;
						static_code = static_code * prime + j;
						static_code = static_code * prime + obj.obsID;
						static_code = static_code * prime + obj.itype;
						break;
					}
					case Types.TYPE_RESOURCE:
					{
						resource_code = resource_code * prime + i;
						resource_code = resource_code * prime + j;
						resource_code = resource_code * prime + obj.obsID;
						resource_code = resource_code * prime + obj.itype;
						break;
					}
					default:
						break;
					}
				}
			}
		}

		HashMap<Integer, Integer> res;
		res = so.getAvatarResources();
		for (int key : res.keySet())
		{
			avator_code = avator_code * prime + key;
			avator_code = avator_code * prime + res.get(key);
		}


		return ""+avator_code + "" + static_code + "" + resource_code + "" + portal_code + "" + movable_code;
	}

	public long GuessHash(ACTIONS act)
	{
		long result = 11;
		long prime = 7;
		ArrayList<Observation>[][] list;
		Vector2d pos;
		double x,y;
		Vector2d orient;
		list = so.getObservationGrid();
		pos = so.getAvatarPosition();
		x = pos.x / (double)so.getBlockSize();
		y = pos.y / (double)so.getBlockSize();
		switch (act) {
		case ACTION_LEFT:
		{
			x -= 1.0;
			break;
		}
		case ACTION_RIGHT:
		{
			x += 1.0;
			break;
		}
		case ACTION_UP:
		{
			y -= 1.0;
			break;
		}
		case ACTION_DOWN:
		{
			y += 1.0;
			break;
		}
		default:
			break;
		}

		if (x < 0)
		{
			x = 0;
		}
		else if (x > list.length)
		{
			x = list.length;
		}
		if (y < 0)
		{
			y = 0;
		}
		else if (y > list[0].length)
		{
			y = list[0].length;
		}

		orient = so.getAvatarOrientation();
		result = result * prime + so.getAvatarType();
		result = result * prime + Double.doubleToLongBits(x);
		result = result * prime + Double.doubleToLongBits(y);
		result = result * prime + Double.doubleToLongBits(orient.x);
		result = result * prime + Double.doubleToLongBits(orient.y);

		list = so.getObservationGrid();
		for (int i = 0; i < list.length; i++)
		{
			result = result*prime + i;
			for (int j = 0; j < list[i].length; j++)
			{
				result = result * prime + j;
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

		HashMap<Integer, Integer> res;
		res = so.getAvatarResources();
		for (int key : res.keySet())
		{
			result = result * prime + key;
			result = result * prime + res.get(key);
		}

		return result;
	}

	public long GetHash()
	{
		return hashcode;
	}

	public double getPriority() {
		int total = 0;

		total += so.getGameTick() * 0.1 - 2 * so.getGameScore() - 0.1 * rand.nextDouble();
		//total += so.getGameTick() * 0.1 - so.getGameScore() - 0.1 * rand.nextDouble() + PenaltyMovableConear(so);

		total += penaltyScore();
		total += NumberOfResources();
		total += DistanceToNextMovable();
		total += PenaltyOverlapPortal();

		return total;
	}

	public double penaltyScore(){
		if(parent.so==null){
			return 0;
		}
		if (parent.so.getGameScore() <= so.getGameScore()){
			return 0;
		}
		else {
			return 20 * parent.so.getGameScore();
		}
	}

	public double NumberOfResources(){
		ArrayList<Observation>[] observations = so.getResourcesPositions();
		if (observations == null)
			return 0;

		int sum = 0;
		for( ArrayList<Observation> observationType: observations ) {
			sum += observationType.size();
		}

		return sum;
	}

	public double DistanceToNextMovable(){
	      List<Observation>[] immovables = so.getMovablePositions( so.getAvatarPosition() );

	      if (immovables == null)
	    	  return 0;

	      for( List<Observation> observationTypes: immovables ) {
	    	  for( Observation observation: observationTypes ) {
	                   return Math.abs(observation.position.x - so.getAvatarPosition().x) / so.getBlockSize() + Math.abs(observation.position.y - so.getAvatarPosition().y) / so.getBlockSize();
	           }
	       }

	       return 0;
		}

	public double PenaltyOverlapPortal(){
		List<Observation>[] portals = so.getPortalsPositions();
		List<Observation>[] movables = so.getMovablePositions();

		if(portals==null || movables==null){
			return 0;
		}
		for(List<Observation> portalObservations : portals){
			for(Observation portal : portalObservations){
				for(List<Observation> movableObservations : movables){
					for(Observation movable : movableObservations){
						if(portal.position.x == movable.position.x && portal.position.y == movable.position.y){
							return 200;
						}
					}
				}
			}
		}
		return 0;
	}
	
//	public double spaceOfRisingScore(StateObservation now, int count){
//		StateObservation so= now.copy();
//		double score = so.getGameScore();
//		double simScore;
//		ArrayList<ACTIONS> actions;
//		Vector2d position = so.getAvatarPosition();
//		Vector2d simPosition;
//		StateObservation sim;
//        int movableObjectStateBefore = hash( so.getMovablePositions() );
////		System.out.println("spaceOfRisingScore " + count);
//
//		actions = so.getAvailableActions();
////		if(actions==null){
////			return false;
////		}
////		else{
////			for(ACTIONS act : actions){
////						System.out.println(act);						
////			}
////		}
//		for(ACTIONS act : actions){
//
//			sim = so.copy();
//			sim.advance(act);
//			simPosition = sim.getAvatarPosition();
//			simScore = sim.getGameScore();
//
////			if(position.x == simPosition.x && position.y == simPosition.y){
////				return 100;
////			}
//	        int movableObjectStateAfter = hash(so.getMovablePositions());
//	        if (position.equals(simPosition)  && simScore == score /*&& movableObjectStateAfter == movableObjectStateBefore*/){
//	            return 500;
//	        }
////			if (!walkAway.advanceStateAndCheckIfIsNilMove(beforeSim, act)){
////				return true;
////			}
////			
////			else if (count>3){
////			//	count=0;
////				return false;
////			}
////			else {
////				count++;
////				spaceOfRisingScore(beforeSim.copy(),count);
////			}
////			
////			beforeSim.advance(act);
////			beforePosition = sim.getAvatarPosition();
////			
////			if(position.x == beforePosition.x && position.y == beforePosition.y){
////				return false;
////			}
//		}
//		return 0;
//
////		return false;
////		for(int i=0; i<path.size(); i++){  //æœ€é�©è§£ã‚’ã‚·ãƒŸãƒ¥ãƒ¬ãƒ¼ãƒˆ
////			so.advance(path.get(i));
////		}
////		beforeScore = so.getGameScore();
////
////		for(int i=0; i<5; i++){  //ä½•ã‚¹ãƒ†ãƒƒãƒ—ã�‹ãƒ©ãƒ³ãƒ€ãƒ ã�«è¡Œå‹•
////			actions = so.getAvailableActions();
////			if(actions==null){
////				break;
////			}
////			Collections.shuffle(actions);
////			so.advance(actions.get(0));
////		}
////		afterScore = so.getGameScore();
////
////		if(afterScore > beforeScore){  //ã‚¹ã‚³ã‚¢ä¸Šæ˜‡ã�®è¦‹è¾¼ã�¿ã�Œã�‚ã‚‹
////			return true;
////		}
////
////		return false;
//	}
//    public static int hash(List<Observation>[] observations) {
//        if (observations == null)
//            return 0;
//
//        int sum = 0;
//        for (List<Observation> observationsOfSameType : observations) {
//            for (Observation anObservations : observationsOfSameType) {
//                sum += hash( anObservations );
//            }
//        }
//
//        return sum;
//    }
//
//    public static int hash( Observation anObservation ) {
//        return (int) (anObservation.obsID * (anObservation.position.x + anObservation.position.y));
//    }

//	public double PenaltyMovableConear(StateObservation so){
//		double value = 0;
//		ArrayList<Observation>[][] list;
//		list = so.getObservationGrid();
//		for (int i = 0; i < list.length; i++)
//		{
//			for (int j = 0; j < list[i].length; j++)
//			{
//				for (Observation obj : list[i][j])
//				{
//					if (obj.category != Types.TYPE_AVATAR)
//					{
//						int count = 0;
//						int temp = 0;
//						if(obj.category==Types.TYPE_MOVABLE){
//							//éš£æŽ¥ã�™ã‚‹ã‚ªãƒ–ã‚¸ã‚§ã‚¯ãƒˆã�ŒStatic ã�‹ã�¤ã€€(å£�éš› ã�¾ã�Ÿã�¯ 3æ–¹ã‚’å›²ã�¾ã‚Œã�¦ã�„ã‚‹)
//							if(i+1<=list.length-1){
//								for (Observation obj1 : list[i+1][j])
//								{
//									if(obj1.category==Types.TYPE_STATIC){
//										count++;
//										temp += 2;
//										if(i+1==list.length-1){
//											value += 1.0;
//										}
//									}
//								}
//							}
//							if(i-1>=0){
//								for (Observation obj1 : list[i-1][j])
//								{
//									if(obj1.category==Types.TYPE_STATIC){
//										count++;
//										temp += 2;
//										if(i-1==0){
//											value += 1.0;
//										}
//									}
//								}
//							}
//							if(j+1<=list[i].length-1){
//								for (Observation obj1 : list[i][j+1])
//								{
//									if(obj1.category==Types.TYPE_STATIC){
//										count++;
//										temp += 3;
//										if(j+1==list[i].length){
//											value += 1.0;
//										}
//									}
//								}
//							}
//							if(j-1>=0){
//								for (Observation obj1 : list[i][j-1])
//								{
//									count++;
//									temp += 3;
//									if(obj1.category==Types.TYPE_STATIC){
//										if(j-1==0){
//											value += 1.0;
//										}
//									}
//								}
//							}
//							if(count!=0)
//							if(temp % count != 0){
//								value += 1.0;
//							}
//						}
//					}
//				}
//			}
//		}
//		return value;
//	}

}
