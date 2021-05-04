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
  public static boolean TIME_LIMITATION;
  public static boolean SELECT_HIGHEST_SCORE_CHILD;
  public static int TREE_SEARCH_SIZE;
  public static int TIME_LIMITATION_IN_MILLIS;
  public static int SIMULATION_DEPTH;
  public static int MAX_DEPTH;

  // Enhancements
  public static boolean TREE_REUSE;
  public static boolean LOSS_AVOIDANCE;
  public static boolean RAW_GAME_SCORE;
  public static boolean EXPAND_ALL_CHILD_NODES;
  public static boolean SAFETY_PREPRUNNING;
  public static boolean EARLY_INITIALIZATION;

  public static int RAW_SCORE_WEIGHT;
  public static int TOTAL_RESOURCES_SCORE_WEIGHT;
  public static int RESOURCE_SCORE_WEIGHT;
  public static int EXPLORATION_SCORE_WEIGHT;
  public static int MOVABLES_SCORE_WEIGHT;
  public static int PORTALS_SCORE_WEIGHT;

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
    this.EPISODES = EPISODES;
  }

  public int getLEVEL() {
    return LEVEL;
  }

  public void setLEVEL(final int LEVEL) {
    this.LEVEL = LEVEL;
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

  public boolean isDEBUG_VIEWER() {
    return DEBUG_VIEWER;
  }

  public void setDEBUG_VIEWER(final boolean DEBUG_VIEWER) {
    this.DEBUG_VIEWER = DEBUG_VIEWER;
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

  public float getSARSA_ALFA() {
    return SARSA_ALFA;
  }

  public void setSARSA_ALFA(final float SARSA_ALFA) {
    this.SARSA_ALFA = SARSA_ALFA;
  }

  public float getSARSA_GAMMA() {
    return SARSA_GAMMA;
  }

  public void setSARSA_GAMMA(final float SARSA_GAMMA) {
    this.SARSA_GAMMA = SARSA_GAMMA;
  }

  public float getSARSA_EPSILON() {
    return SARSA_EPSILON;
  }

  public void setSARSA_EPSILON(final float SARSA_EPSILON) {
    this.SARSA_EPSILON = SARSA_EPSILON;
  }

  public boolean isTIME_LIMITATION() {
    return TIME_LIMITATION;
  }

  public void setTIME_LIMITATION(final boolean TIME_LIMITATION) {
    this.TIME_LIMITATION = TIME_LIMITATION;
  }

  public boolean isSELECT_HIGHEST_SCORE_CHILD() {
    return SELECT_HIGHEST_SCORE_CHILD;
  }

  public void setSELECT_HIGHEST_SCORE_CHILD(final boolean SELECT_HIGHEST_SCORE_CHILD) {
    this.SELECT_HIGHEST_SCORE_CHILD = SELECT_HIGHEST_SCORE_CHILD;
  }

  public int getTREE_SEARCH_SIZE() {
    return TREE_SEARCH_SIZE;
  }

  public void setTREE_SEARCH_SIZE(final int TREE_SEARCH_SIZE) {
    this.TREE_SEARCH_SIZE = TREE_SEARCH_SIZE;
  }

  public int getTIME_LIMITATION_IN_MILLIS() {
    return TIME_LIMITATION_IN_MILLIS;
  }

  public void setTIME_LIMITATION_IN_MILLIS(final int TIME_LIMITATION_IN_MILLIS) {
    this.TIME_LIMITATION_IN_MILLIS = TIME_LIMITATION_IN_MILLIS;
  }

  public int getSIMULATION_DEPTH() {
    return SIMULATION_DEPTH;
  }

  public void setSIMULATION_DEPTH(final int SIMULATION_DEPTH) {
    this.SIMULATION_DEPTH = SIMULATION_DEPTH;
  }

  public int getMAX_DEPTH() {
    return MAX_DEPTH;
  }

  public void setMAX_DEPTH(final int MAX_DEPTH) {
    this.MAX_DEPTH = MAX_DEPTH;
  }

  public boolean isTREE_REUSE() {
    return TREE_REUSE;
  }

  public void setTREE_REUSE(final boolean TREE_REUSE) {
    this.TREE_REUSE = TREE_REUSE;
  }

  public boolean isLOSS_AVOIDANCE() {
    return LOSS_AVOIDANCE;
  }

  public void setLOSS_AVOIDANCE(final boolean LOSS_AVOIDANCE) {
    this.LOSS_AVOIDANCE = LOSS_AVOIDANCE;
  }

  public boolean isRAW_GAME_SCORE() {
    return RAW_GAME_SCORE;
  }

  public void setRAW_GAME_SCORE(final boolean RAW_GAME_SCORE) {
    this.RAW_GAME_SCORE = RAW_GAME_SCORE;
  }

  public boolean isEXPAND_ALL_CHILD_NODES() {
    return EXPAND_ALL_CHILD_NODES;
  }

  public void setEXPAND_ALL_CHILD_NODES(final boolean EXPAND_ALL_CHILD_NODES) {
    this.EXPAND_ALL_CHILD_NODES = EXPAND_ALL_CHILD_NODES;
  }

  public boolean isSAFETY_PREPRUNNING() {
    return SAFETY_PREPRUNNING;
  }

  public void setSAFETY_PREPRUNNING(final boolean SAFETY_PREPRUNNING) {
    this.SAFETY_PREPRUNNING = SAFETY_PREPRUNNING;
  }

  public boolean isEARLY_INITIALIZATION() {
    return EARLY_INITIALIZATION;
  }

  public void setEARLY_INITIALIZATION(final boolean EARLY_INITIALIZATION) {
    this.EARLY_INITIALIZATION = EARLY_INITIALIZATION;
  }

  public int getRAW_SCORE_WEIGHT() {
    return RAW_SCORE_WEIGHT;
  }

  public void setRAW_SCORE_WEIGHT(final int RAW_SCORE_WEIGHT) {
    this.RAW_SCORE_WEIGHT = RAW_SCORE_WEIGHT;
  }

  public int getTOTAL_RESOURCES_SCORE_WEIGHT() {
    return TOTAL_RESOURCES_SCORE_WEIGHT;
  }

  public void setTOTAL_RESOURCES_SCORE_WEIGHT(final int TOTAL_RESOURCES_SCORE_WEIGHT) {
    this.TOTAL_RESOURCES_SCORE_WEIGHT = TOTAL_RESOURCES_SCORE_WEIGHT;
  }

  public int getRESOURCE_SCORE_WEIGHT() {
    return RESOURCE_SCORE_WEIGHT;
  }

  public void setRESOURCE_SCORE_WEIGHT(final int RESOURCE_SCORE_WEIGHT) {
    this.RESOURCE_SCORE_WEIGHT = RESOURCE_SCORE_WEIGHT;
  }

  public int getEXPLORATION_SCORE_WEIGHT() {
    return EXPLORATION_SCORE_WEIGHT;
  }

  public void setEXPLORATION_SCORE_WEIGHT(final int EXPLORATION_SCORE_WEIGHT) {
    this.EXPLORATION_SCORE_WEIGHT = EXPLORATION_SCORE_WEIGHT;
  }

  public int getMOVABLES_SCORE_WEIGHT() {
    return MOVABLES_SCORE_WEIGHT;
  }

  public void setMOVABLES_SCORE_WEIGHT(final int MOVABLES_SCORE_WEIGHT) {
    this.MOVABLES_SCORE_WEIGHT = MOVABLES_SCORE_WEIGHT;
  }

  public int getPORTALS_SCORE_WEIGHT() {
    return PORTALS_SCORE_WEIGHT;
  }

  public void setPORTALS_SCORE_WEIGHT(final int PORTALS_SCORE_WEIGHT) {
    this.PORTALS_SCORE_WEIGHT = PORTALS_SCORE_WEIGHT;
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
