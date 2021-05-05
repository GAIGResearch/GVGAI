package tracks.singlePlayer.florabranchi.database;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.TreeMap;

import tracks.singlePlayer.florabranchi.agents.meta.EMetaActions;
import tracks.singlePlayer.florabranchi.agents.meta.GameOptions;
import tracks.singlePlayer.florabranchi.agents.meta.MetaWeights;
import tracks.singlePlayer.florabranchi.agents.meta.RunOptions;

public class DatabaseClientTest {

  @Before
  public void setUp() throws Exception {

  }

  @Test
  @Ignore
  public void createTables() {

    final DatabaseClient databaseClient = new DatabaseClient();
    ExecutionEpisodeResultsDAO dao = new ExecutionEpisodeResultsDAO(databaseClient);
    MetaWeightsDAO metaWeightsDAO = new MetaWeightsDAO(databaseClient);
    metaWeightsDAO.createMetaWeightsTable();
    //dao.createEpisodeTable();
  }

  @Test
  public void testAddEpisode() {

    GameOptions gameOptions = new GameOptions();
    gameOptions.treeReuse = true;
/*    gameOptions.lossAvoidance = true;
    gameOptions.expandAllNodes = true;
    gameOptions.safetyPreprunning = true;
    gameOptions.shallowRollout = true;*/
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

  @Test
  public void testSaveWeights() {

    RunOptions runOptions = new RunOptions();
    runOptions.game = "aliens";
    runOptions.level = 1;
    runOptions.totalGames = 1;
    runOptions.scores = new int[5];
    runOptions.scores[1] = 50;
    runOptions.scores[2] = 40;
    runOptions.scores[3] = 30;
    runOptions.wr = 30.5;

    MetaWeights metaWeights = new MetaWeights();
    TreeMap treeMap = new TreeMap<>();
    treeMap.put("1", 123);
    //metaWeights.getWeightVectorMap().put(EMetaActions.IS_ALIENS, treeMap);

    SavedMetaWeights savedMetaWeights = new SavedMetaWeights(runOptions);
    savedMetaWeights.metaWeights = metaWeights;

    MetaWeightsDAO metaWeightsDAO = new MetaWeightsDAO(new DatabaseClient());
    metaWeightsDAO.save(metaWeights);
  }

  @Test
  public void testGetWeights() {

    MetaWeightsDAO metaWeightsDAO = new MetaWeightsDAO(new DatabaseClient());
    final MetaWeights metaWeights = metaWeightsDAO.getMetaWeights(1);
    System.out.println(222);
  }

}