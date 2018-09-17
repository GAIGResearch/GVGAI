package tutorialGeneration;

import core.game.Game;
import core.game.GameDescription;
import core.game.SLDescription;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import tools.GameAnalyzer;
import tools.IO;
import tools.LevelAnalyzer;

public class AtDelfi {

	private boolean verbose;
	
	private AtDelfiGraph gameGraph;
	private SLDescription sl;
	private GameDescription gd;
	private LevelAnalyzer la;
	private GameAnalyzer ga;
	
	private Game game;
	private String[] lines; 
	
	private int seed;
	
	private String gameName;
	private String gameFile;
	private String levelFile;
	
	public AtDelfi(String gameFile, String levelFile, String gameName, int seed, boolean verbose) {
		this.verbose = verbose;
		if (this.verbose) 
			System.out.println("Initializing AtDelfi...");
		this.seed = seed;
		
		this.gameFile = gameFile;
		this.levelFile = levelFile;
		this.gameName = gameName;
		
		// build game, game description, and slDescription
		this.game = getGame();
		this.lines = getLevelLines();
		
		this.gd = new GameDescription(this.game);
		
		try {
			this.sl = new SLDescription(this.game, this.lines, this.seed);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.la = new LevelAnalyzer(sl);
		this.ga = new GameAnalyzer(gd);
		
		this.buildGraph();
	}
	
	public void buildGraph() {
		this.gameGraph = new AtDelfiGraph(gd, sl, ga, la);
		this.gameGraph.build();
	}

	public Game getGame() {
        VGDLFactory.GetInstance().init(); // This always first thing to do.
        VGDLRegistry.GetInstance().init();
	
        System.out.println(
                "** Generating the game from " + this.gameName + " **");
        
        // First, we create the game to be played..
        Game toPlay = new VGDLParser().parseGame(this.gameFile);
        return toPlay;
	}
	
	public String[] getLevelLines() {
		String[] lines = new IO().readFile(this.levelFile);
		return lines;
	}
	
	/**
	 * @return the gameGraph
	 */
	public AtDelfiGraph getGameGraph() {
		return gameGraph;
	}


	/**
	 * @param gameGraph the gameGraph to set
	 */
	public void setGameGraph(AtDelfiGraph gameGraph) {
		this.gameGraph = gameGraph;
	}
}	
