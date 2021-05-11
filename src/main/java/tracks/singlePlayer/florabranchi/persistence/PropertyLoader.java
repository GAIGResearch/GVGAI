package tracks.singlePlayer.florabranchi.persistence;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;


public class PropertyLoader {

  public static boolean initialized;

  public static int GAME;
  public static String GAME_NAME;
  public static int EPISODES;
  public static int LEVEL;

  public static String AGENT;
  public static Level LOGGER_LEVEL;

  public static boolean LOAD_RUN_INSTRUCTIONS;

  public boolean SAVE_RESULTS;
  public static boolean VISUALS;
  public static boolean SHOW_TREE;

  // MCTS
  public static boolean TIME_LIMITATION;
  public static boolean SELECT_HIGHEST_SCORE_CHILD;
  public static int TREE_SEARCH_SIZE;
  public static int TIME_LIMITATION_IN_MILLIS;
  public static int ROLLOUT_DEPTH;
  public static boolean SHALLOW_ROLLOUT;
  // global to save node exploration
  public static int AVERAGE_NODES = 0;

  // Enhancements
  public static boolean TREE_REUSE;
  public static boolean MACRO_ACTIONS;
  public static boolean LOSS_AVOIDANCE;
  public static boolean RAW_GAME_SCORE;
  public static boolean EARLY_INITIALIZATION;

  public PropertyLoader(final String configFile) throws IOException {

    InputStream inputStream;
    Properties prop = new Properties();

    inputStream = getClass().getClassLoader().getResourceAsStream(configFile);

    if (inputStream != null) {
      prop.load(inputStream);
    } else {
      throw new FileNotFoundException("property file '" + configFile + "' not found in the classpath");
    }

    if (initialized) {
      return;
    }


    final String loggerLevel = prop.getProperty("LOGGER_LEVEL");
    LOGGER_LEVEL = Level.parse(loggerLevel);
    Date time = new Date(System.currentTimeMillis());

    LOAD_RUN_INSTRUCTIONS = Boolean.parseBoolean(prop.getProperty("LOAD_RUN_INSTRUCTIONS"));

    if (!LOAD_RUN_INSTRUCTIONS) {
      GAME = castGame(prop.getProperty("GAME"));
      GAME_NAME = prop.getProperty("GAME");
      LEVEL = Integer.parseInt(prop.getProperty("LEVEL"));

      TREE_REUSE = Boolean.parseBoolean(prop.getProperty("TREE_REUSE", "false"));
      SHALLOW_ROLLOUT = Boolean.parseBoolean(prop.getProperty("SHALLOW_ROLLOUT", "false"));
      MACRO_ACTIONS = Boolean.parseBoolean(prop.getProperty("MACRO_ACTIONS", "false"));
      LOSS_AVOIDANCE = Boolean.parseBoolean(prop.getProperty("LOSS_AVOIDANCE", "false"));
      RAW_GAME_SCORE = Boolean.parseBoolean(prop.getProperty("RAW_GAME_SCORE", "false"));
      EARLY_INITIALIZATION = Boolean.parseBoolean(prop.getProperty("EARLY_INITIALIZATION", "false"));
    }

    EPISODES = Integer.parseInt(prop.getProperty("EPISODES"));

    // Agent
    AGENT = castAgentPath(prop.getProperty("AGENT"));
    VISUALS = Boolean.parseBoolean(prop.getProperty("VISUALS"));
    SHOW_TREE = Boolean.parseBoolean(prop.getProperty("SHOW_TREE"));

    // Monte Carlo properties, if required
    TIME_LIMITATION = Boolean.parseBoolean(prop.getProperty("TIME_LIMITATION", "false"));
    SELECT_HIGHEST_SCORE_CHILD = Boolean.parseBoolean(prop.getProperty("SELECT_HIGHEST_SCORE_CHILD", "true"));
    TIME_LIMITATION_IN_MILLIS = Integer.parseInt(prop.getProperty("TIME_LIMITATION_IN_MILLIS", "40"));
    TREE_SEARCH_SIZE = Integer.parseInt(prop.getProperty("TREE_SEARCH_SIZE", "0"));
    ROLLOUT_DEPTH = Integer.parseInt(prop.getProperty("ROLLOUT_DEPTH", "0"));

    initialized = true;
  }

  public int getGAME() {
    return GAME;
  }

  public void setGAME(final int GAME) {
    this.GAME = GAME;
  }

  public int getEPISODES() {
    return EPISODES;
  }

  public void setEPISODES(final int EPISODES) {
    PropertyLoader.EPISODES = EPISODES;
  }

  public int getLEVEL() {
    return LEVEL;
  }

  public void setLEVEL(final int LEVEL) {
    PropertyLoader.LEVEL = LEVEL;
  }

  public String getAGENT() {
    return AGENT;
  }

  public void setAGENT(final String AGENT) {
    this.AGENT = AGENT;
  }

  public Level getLOGGER_LEVEL() {
    return LOGGER_LEVEL;
  }

  public void setLOGGER_LEVEL(final Level LOGGER_LEVEL) {
    this.LOGGER_LEVEL = LOGGER_LEVEL;
  }


  public boolean isSAVE_RESULTS() {
    return SAVE_RESULTS;
  }

  public void setSAVE_RESULTS(final boolean SAVE_RESULTS) {
    this.SAVE_RESULTS = SAVE_RESULTS;
  }

  public boolean isVISUALS() {
    return VISUALS;
  }

  public void setVISUALS(final boolean VISUALS) {
    this.VISUALS = VISUALS;
  }

  public boolean isSHOW_TREE() {
    return SHOW_TREE;
  }

  public void setSHOW_TREE(final boolean SHOW_TREE) {
    this.SHOW_TREE = SHOW_TREE;
  }

  public boolean isTIME_LIMITATION() {
    return TIME_LIMITATION;
  }

  public void setTIME_LIMITATION(final boolean TIME_LIMITATION) {
    PropertyLoader.TIME_LIMITATION = TIME_LIMITATION;
  }

  public boolean isSELECT_HIGHEST_SCORE_CHILD() {
    return SELECT_HIGHEST_SCORE_CHILD;
  }

  public void setSELECT_HIGHEST_SCORE_CHILD(final boolean SELECT_HIGHEST_SCORE_CHILD) {
    PropertyLoader.SELECT_HIGHEST_SCORE_CHILD = SELECT_HIGHEST_SCORE_CHILD;
  }

  public int getTREE_SEARCH_SIZE() {
    return TREE_SEARCH_SIZE;
  }

  public void setTREE_SEARCH_SIZE(final int TREE_SEARCH_SIZE) {
    PropertyLoader.TREE_SEARCH_SIZE = TREE_SEARCH_SIZE;
  }

  public int getTIME_LIMITATION_IN_MILLIS() {
    return TIME_LIMITATION_IN_MILLIS;
  }

  public void setTIME_LIMITATION_IN_MILLIS(final int TIME_LIMITATION_IN_MILLIS) {
    PropertyLoader.TIME_LIMITATION_IN_MILLIS = TIME_LIMITATION_IN_MILLIS;
  }

  public boolean isTREE_REUSE() {
    return TREE_REUSE;
  }

  public void setTREE_REUSE(final boolean TREE_REUSE) {
    PropertyLoader.TREE_REUSE = TREE_REUSE;
  }

  public boolean isLOSS_AVOIDANCE() {
    return LOSS_AVOIDANCE;
  }

  public void setLOSS_AVOIDANCE(final boolean LOSS_AVOIDANCE) {
    PropertyLoader.LOSS_AVOIDANCE = LOSS_AVOIDANCE;
  }

  public boolean isRAW_GAME_SCORE() {
    return RAW_GAME_SCORE;
  }

  public void setRAW_GAME_SCORE(final boolean RAW_GAME_SCORE) {
    PropertyLoader.RAW_GAME_SCORE = RAW_GAME_SCORE;
  }

  public boolean isEARLY_INITIALIZATION() {
    return EARLY_INITIALIZATION;
  }

  public void setEARLY_INITIALIZATION(final boolean EARLY_INITIALIZATION) {
    PropertyLoader.EARLY_INITIALIZATION = EARLY_INITIALIZATION;
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
      case "base_monte_carlo":
        return "tracks.singlePlayer.florabranchi.agents.BaseMCTSAgent";
      case "sarsa_trainer":
        return "tracks.singlePlayer.florabranchi.trash.SarsaTrainerAgent";
      case "monte_carlo_visuals":
        return "tracks.singlePlayer.florabranchi.trash.MCTSVisualsAgent";
      case "parametrized_monte_carlo_visuals":
        return "tracks.singlePlayer.florabranchi.agents.ParametrizedMonteCarloTreeAgent";
      case "monte_carlo":
      default:
        return "tracks.singlePlayer.florabranchi.agents.MonteCarloTreeAgent";
    }
  }

}
