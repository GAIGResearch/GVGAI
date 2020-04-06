package tracks.singlePlayer.florabranchi.persistence;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

public class PropertyLoader {

  public int GAME;
  public int EPISODES;
  public int LEVEL;

  public String AGENT;

  public boolean VISUALS;
  public boolean SAVE_RESULTS;

  public float SARSA_ALFA;
  public float SARSA_GAMMA;
  public float SARSA_EPISLON;

  public PropertyLoader(final String configFile) throws IOException {

    InputStream inputStream;
    Properties prop = new Properties();

    inputStream = getClass().getClassLoader().getResourceAsStream(configFile);

    if (inputStream != null) {
      prop.load(inputStream);
    } else {
      throw new FileNotFoundException("property file '" + configFile + "' not found in the classpath");
    }

    Date time = new Date(System.currentTimeMillis());

    // get the property value and print it out
    GAME = castGame(prop.getProperty("GAME"));
    EPISODES = Integer.parseInt(prop.getProperty("LEVEL"));
    LEVEL = Integer.parseInt(prop.getProperty("EPISODES"));

    // Agent
    AGENT = castAgentPath(prop.getProperty("AGENT"));
    VISUALS = Boolean.parseBoolean(prop.getProperty("VISUALS"));

    // Monte Carlo properties, if required

    // Sarsa properties, if required
    SARSA_ALFA = Float.parseFloat(prop.getProperty("EPISODES"));
    SARSA_GAMMA = Float.parseFloat(prop.getProperty("EPISODES"));
    SARSA_EPISLON = Float.parseFloat(prop.getProperty("EPISODES"));


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
      case "monte_carlo":
      default:
        return "tracks.singlePlayer.florabranchi.agents.MonteCarloTreeAgent";
    }
  }

}
