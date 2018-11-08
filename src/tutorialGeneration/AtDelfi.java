package tutorialGeneration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.graphstream.graph.Graph;
import org.json.simple.parser.ParseException;

import core.game.Game;
import core.game.GameDescription;
import core.game.SLDescription;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;
import tools.GameAnalyzer;
import tools.IO;
import tools.LevelAnalyzer;
import tutorialGeneration.criticalPathing.CriticalPather;
import video.basics.BunchOfGames;

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
	
	private VisualDemonstrationInterfacer vdi;
		
	private String[] agents = {"adrienctx.Agent", "NovelTS.Agent", "NovTea.Agent", "Number27.Agent", "YOLOBOT.Agent", "tracks.singlePlayer.simple.doNothing.Agent", "tracks.singlePlayer.simple.sampleonesteplookahead.Agent"};
//	private String[] agents = {"adrienctx.Agent"};

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
		
		try {
			this.vdi = new VisualDemonstrationInterfacer(this.gameName, false);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		this.buildGraph();
	}
	
	public void buildGraph() {
		this.gameGraph = new AtDelfiGraph(gd, sl, ga, la);
		Graph graph = this.gameGraph.build();
		changeGraphTitle(graph);
		this.gameGraph.insertFrameInfo(vdi, agents);
		
		if(gameGraph.isMechanicVisualization()) {
			gameGraph.visualizeMechanicGraph();
		}
		if(gameGraph.isNodeVisualization()) {
			gameGraph.visualizeNodeGraph();
		}
	}

	public void testPlayGames() {
		ArrayList<BunchOfGames> bogs = new ArrayList<>();
		for(int i = 0; i < 1; i++) {
			for(int j = 0; j < 1; j++) {
				bogs.add(new BunchOfGames(gameFile, levelFile, agents[i]));
			}
		}
		try {
			vdi.runBunchOfGames(bogs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Mechanic> criticalPath(CriticalPather criticalPather, String agent, boolean isWin) {
		List<Mechanic> criticalPath = criticalPather.findCriticalPath(agent, isWin);
		
		return criticalPath;
	}
	
	
	public void playGames() {
		ArrayList<BunchOfGames> bogs = new ArrayList<>();
		for(int i = 0; i < agents.length; i++) {
			for(int j = 0; j < 5; j++) {
				bogs.add(new BunchOfGames(gameFile, levelFile, agents[i]));
			}
		}
		try {
			vdi.runBunchOfGames(bogs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void changeGraphTitle(Graph graph) {
		String title = this.gameName;
		Border border = BorderFactory.createTitledBorder(title);
		graph.display().getDefaultView().setBorder(border);
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
