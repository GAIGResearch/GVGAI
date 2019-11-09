package video.basics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import tutorialGeneration.Mechanic;
import video.handlers.FrameInteractionAssociation;

public class GameSummary {

	public int simulationNumber;
	public int result;
	public HashMap<String, Integer> mapMechanicsToFrames;
	public HashMap<Integer, int[]> relevantFrames;

	public GameSummary(){}

	public GameSummary(int simulationNumber,
			int result,
			HashMap<String, Integer> mapMechanicsToFrames,
			HashMap<Integer, int[]> relevantFrames)
	{
		this.simulationNumber = simulationNumber;
		this.result = result;
		this.mapMechanicsToFrames = mapMechanicsToFrames;
		this.relevantFrames = relevantFrames;
	}

}
