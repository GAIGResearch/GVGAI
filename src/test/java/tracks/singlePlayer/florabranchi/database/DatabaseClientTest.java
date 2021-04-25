package tracks.singlePlayer.florabranchi.database;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import tracks.singlePlayer.florabranchi.agents.meta.GameOptions;
import tracks.singlePlayer.florabranchi.agents.meta.RunOptions;

public class DatabaseClientTest {

  @Before
  public void setUp() throws Exception {

  }

  @Test
  @Ignore
  public void createTables() {

    ExecutionEpisodeResultsDAO dao = new ExecutionEpisodeResultsDAO(new DatabaseClient());
    dao.createEpisodeTable();
  }

  @Test
  public void testAddEpisode() {

    GameOptions gameOptions = new GameOptions();
    gameOptions.reuseTree = true;
    gameOptions.lossAvoidance = true;
    gameOptions.expandAllNodes = true;
    gameOptions.safetyPreprunning = true;
    gameOptions.shallowRollout = true;
    gameOptions.rawGameScore = true;

    RunOptions runOptions = new RunOptions();
    runOptions.game = "aliens";
    runOptions.level = 1;
    runOptions.totalGames = 1;
    runOptions.scores = new int[5];
    runOptions.scores[1] = 50;
    runOptions.scores[2] = 40;
    runOptions.scores[3] = 30;
    runOptions.wr = 30.5;

    runOptions.gameOptions = gameOptions;

    ExecutionEpisodeResults executionEpisodeResults = new
        ExecutionEpisodeResults(runOptions);


    ExecutionEpisodeResultsDAO dao = new ExecutionEpisodeResultsDAO(new DatabaseClient());

    dao.save(executionEpisodeResults);


  }

}