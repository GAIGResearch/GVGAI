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
  public int TREE_SEARCH_SIZE;
  public int SIMULATION_DEPTH;

  // Enhancements
  public boolean TREE_REUSE;
  public boolean LOSS_AVOIDANCE;
  public boolean RAW_GAME_SCORE;

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
    TREE_SEARCH_SIZE = Integer.parseInt(prop.getProperty("TREE_SEARCH_SIZE", "0"));
    SIMULATION_DEPTH = Integer.parseInt(prop.getProperty("SIMULATION_DEPTH", "0"));

    TREE_REUSE = Boolean.parseBoolean(prop.getProperty("TREE_REUSE", "false"));
    LOSS_AVOIDANCE = Boolean.parseBoolean(prop.getProperty("LOSS_AVOIDANCE", "false"));
    RAW_GAME_SCORE = Boolean.parseBoolean(prop.getProperty("RAW_GAME_SCORE", "false"));

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
      case "sarsa":
        return "tracks.singlePlayer.florabranchi.agents.SarsaAgent";
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
