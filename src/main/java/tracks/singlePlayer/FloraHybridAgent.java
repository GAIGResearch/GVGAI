package tracks.singlePlayer;

import java.io.IOException;

import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

public class FloraHybridAgent {

  public static void main(String[] args) throws IOException {

    final String configFile = args[0];

    PropertyLoader loader = new PropertyLoader(configFile);
    System.out.println(loader.GAME);

  }
}
