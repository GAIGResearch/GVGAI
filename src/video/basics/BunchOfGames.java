package video.basics;
/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 24/02/2018
 * @author Tiago Machado
 */
public class BunchOfGames {

	public String gamePath;
	public String gameLevelPath;
	public String playerPath; //agent who will play the game
	
	public String levelCount;
	public String playthroughCount;
	
	public BunchOfGames(String gamePath, String gameLevelPath, String playerPath)
	{
		this.gamePath = gamePath;
		this.gameLevelPath = gameLevelPath;
		this.playerPath = playerPath;
	}
	
	public BunchOfGames(String gamePath, String gameLevelPath, String playerPath, String levelCount, String playthroughCount) {
		this.gamePath = gamePath;
		this.gameLevelPath = gameLevelPath;
		this.playerPath = playerPath;
		this.levelCount = levelCount;
		this.playthroughCount = playthroughCount;
	}
}
