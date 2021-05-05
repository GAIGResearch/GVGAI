package tracks.singlePlayer.florabranchi.database;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class GenerateHibernateTables {

  public static void main(String[] args) {
    EntityManagerFactory factory = Persistence.
        createEntityManagerFactory("base_mcts_results");
    factory.close();
  }
}

