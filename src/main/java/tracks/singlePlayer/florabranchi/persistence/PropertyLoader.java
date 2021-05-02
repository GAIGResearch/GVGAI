package tracks.singlePlayer.florabranchi.persistence;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;

public class PropertyLoader {

  public int GAME;
  public int EPISODES;
  public int LEVEL;

  public String AGENT;
  public Level LOGGER_LEVEL;

  public boolean DEBUG_VIEWER;
  public boolean SAVE_RESULTS;
  public boolean VISUALS;
  public boolean SHOW_TREE;

  public float SARSA_ALFA;
  public float SARSA_GAMMA;
  public float SARSA_EPSILON;

  // MCTS
  public boolean TIME_LIMITATION;
  public boolean SELECT_HIGHEST_SCORE_CHILD;
  public int TREE_SEARCH_SIZE;
  public int TIME_LIMITATION_IN_MILLIS;
  public int SIMULATION_DEPTH;
  public int MAX_DEPTH;

  // Enhancements
  public boolean TREE_REUSE;
  public boolean LOSS_AVOIDANCE;
  public boolean RAW_GAME_SCORE;
  public boolean EXPAND_ALL_CHILD_NODES;
  public boolean SAFETY_PREPRUNNING;
  public boolean EARLY_INITIALIZATION;

  public int RAW_SCORE_WEIGHT;
  public int TOTAL_RESOURCES_SCORE_WEIGHT;
  public int RESOURCE_SCORE_WEIGHT;
  public int EXPLORATION_SCORE_WEIGHT;
  public int MOVABLES_SCORE_WEIGHT;
  public int PORTALS_SCORE_WEIGHT;

  public PropertyLoader(final String configFile) throws IOException {

    InputStream inputStream;
    Properties prop = new Properties();

    inputStream = getClass().getClassLoader().getResourceAsStream(configFile);

    if (inputStream != null) {
      prop.load(inputStream);
    } else {
      throw new FileNotFoundException("property file '" + configFile + "' not found in the classpath");
    }


    final String loggerLevel = prop.getProperty("LOGGER_LEVEL");
    LOGGER_LEVEL = Level.parse(loggerLevel);
    Date time = new Date(System.currentTimeMillis());

    // get the property value and print it out
    GAME = castGame(prop.getProperty("GAME"));
    LEVEL = Integer.parseInt(prop.getProperty("LEVEL"));
    EPISODES = Integer.parseInt(prop.getProperty("EPISODES"));

    // Agent
    AGENT = castAgentPath(prop.getProperty("AGENT"));
    DEBUG_VIEWER = Boolean.parseBoolean(prop.getProperty("DEBUG_VIEWER"));
    VISUALS = Boolean.parseBoolean(prop.getProperty("VISUALS"));
    SHOW_TREE = Boolean.parseBoolean(prop.getProperty("SHOW_TREE"));

    // Monte Carlo properties, if required
    TIME_LIMITATION = Boolean.parseBoolean(prop.getProperty("TIME_LIMITATION", "false"));
    SELECT_HIGHEST_SCORE_CHILD = Boolean.parseBoolean(prop.getProperty("SELECT_HIGHEST_SCORE_CHILD", "true"));
    TIME_LIMITATION_IN_MILLIS = Integer.parseInt(prop.getProperty("TIME_LIMITATION_IN_MILLIS", "40"));
    TREE_SEARCH_SIZE = Integer.parseInt(prop.getProperty("TREE_SEARCH_SIZE", "0"));
    SIMULATION_DEPTH = Integer.parseInt(prop.getProperty("SIMULATION_DEPTH", "0"));
    MAX_DEPTH = Integer.parseInt(prop.getProperty("MAX_DEPTH", "0"));

    TREE_REUSE = Boolean.parseBoolean(prop.getProperty("TREE_REUSE", "false"));
    LOSS_AVOIDANCE = Boolean.parseBoolean(prop.getProperty("LOSS_AVOIDANCE", "false"));
    RAW_GAME_SCORE = Boolean.parseBoolean(prop.getProperty("RAW_GAME_SCORE", "false"));
    EXPAND_ALL_CHILD_NODES = Boolean.parseBoolean(prop.getProperty("EXPAND_ALL_CHILD_NODES", "false"));
    SAFETY_PREPRUNNING = Boolean.parseBoolean(prop.getProperty("SAFETY_PREPRUNNING", "false"));
    EARLY_INITIALIZATION = Boolean.parseBoolean(prop.getProperty("EARLY_INITIALIZATION", "false"));

    // Heuristic weights
    RAW_SCORE_WEIGHT = Integer.parseInt(prop.getProperty("RAW_SCORE_WEIGHT", "1"));
    TOTAL_RESOURCES_SCORE_WEIGHT = Integer.parseInt(prop.getProperty("TOTAL_RESOURCES_SCORE_WEIGHT", "1"));
    RESOURCE_SCORE_WEIGHT = Integer.parseInt(prop.getProperty("RESOURCE_SCORE_WEIGHT", "1"));
    EXPLORATION_SCORE_WEIGHT = Integer.parseInt(prop.getProperty("EXPLORATION_SCORE_WEIGHT", "1"));
    MOVABLES_SCORE_WEIGHT = Integer.parseInt(prop.getProperty("MOVABLES_SCORE_WEIGHT", "1"));
    PORTALS_SCORE_WEIGHT = Integer.parseInt(prop.getProperty("PORTALS_SCORE_WEIGHT", "1"));

    // Sarsa properties, if required
    SARSA_ALFA = Float.parseFloat(prop.getProperty("SARSA_ALFA", "0"));
    SARSA_GAMMA = Float.parseFloat(prop.getProperty("SARSA_GAMMA", "0"));
    SARSA_EPSILON = Float.parseFloat(prop.getProperty("SARSA_EPSILON", "0"));


  }

  public int castGame(final String game) {
    final AvailableGames castedGame = AvailableGames.fromName(game);
    if (game == null) {
      System.out.println("Failed to solve game. Starting Aliens.");
      return 1;
    }
    return Objects.requireNonNull(castedGame).getId();
  }

  public String castAgentPath(final String agentName) {
    switch (agentName) {
      case "sarsa_monte_carlo_visuals":
        return "tracks.singlePlayer.florabranchi.agents.SavedManualExecutionAgent";
      case "sarsa_trainer":
        return "tracks.singlePlayer.florabranchi.agents.SarsaTrainerAgent";
      case "monte_carlo_visuals":
        return "tracks.singlePlayer.florabranchi.agents.MCTSVisualsAgent";
      case "parametrized_monte_carlo_visuals":
        return "tracks.singlePlayer.florabranchi.agents.ParametrizedMonteCarloTreeAgent";
      case "monte_carlo":
      default:
        return "tracks.singlePlayer.florabranchi.agents.MonteCarloTreeAgent";
    }
  }

}
