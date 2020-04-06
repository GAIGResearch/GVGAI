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


  }

  public int castGame(final String game) {
    final AvailableGames castedGame = AvailableGames.fromName(game);
    if (game == null) {
      System.out.println("Failed to solve game. Starting Aliens.");
      return 1;
    }
    return Objects.requireNonNull(castedGame).getId();
  }

}
