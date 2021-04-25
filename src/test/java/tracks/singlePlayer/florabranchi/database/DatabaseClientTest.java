package tracks.singlePlayer.florabranchi.database;


import org.junit.Before;
import org.junit.Test;

public class DatabaseClientTest {

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void createTables() {

    DatabaseClient databaseClient = new DatabaseClient();

    databaseClient.createTables();

  }

}