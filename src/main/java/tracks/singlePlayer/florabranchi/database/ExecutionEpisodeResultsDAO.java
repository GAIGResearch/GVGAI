package tracks.singlePlayer.florabranchi.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class ExecutionEpisodeResultsDAO {

  DatabaseClient databaseClient;

  public ExecutionEpisodeResultsDAO(final DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  public void save(ExecutionEpisodeResults results) {

    String sql = "INSERT INTO episodes(game,level,reuseTree,lossAvoidance, " +
        "expandAllNodes, safetyPreprunning, shallowRollout, totalGames, scores, wins, wr)"
        + " VALUES(?,?,?,?,?,?,?,?,?,?,?)";

    Connection conn = null;
    PreparedStatement pstm = null;

    try {
      //Cria uma conexão com o banco
      conn = databaseClient.getConnection();

      //Cria um PreparedStatment, classe usada para executar a query
      pstm = conn.prepareStatement(sql);

      pstm.setString(1, results.usedOptions.game);
      pstm.setInt(2, results.usedOptions.level);
      pstm.setString(3, String.valueOf(results.usedOptions.gameOptions.treeReuse));
      pstm.setString(4, String.valueOf(results.usedOptions.gameOptions.treeReuse));
      pstm.setString(5, String.valueOf(results.usedOptions.gameOptions.treeReuse));
      pstm.setString(6, String.valueOf(results.usedOptions.gameOptions.treeReuse));
      pstm.setString(7, String.valueOf(results.usedOptions.gameOptions.treeReuse));

      pstm.setInt(8, results.usedOptions.totalGames);
      pstm.setString(9, Arrays.toString(results.usedOptions.scores));
      pstm.setString(10, results.usedOptions.game);
      pstm.setInt(11, results.usedOptions.level);

      //Executa a sql para inserção dos dados
      pstm.execute();

    } catch (Exception e) {

      e.printStackTrace();
    } finally {

      //Fecha as conexões

      try {
        if (pstm != null) {

          pstm.close();
        }

        if (conn != null) {
          conn.close();
        }

      } catch (Exception e) {

        e.printStackTrace();
      }
    }
  }

  public void createEpisodeTable() {

    final Statement stmt;
    Connection conn = null;
    try {
      conn = databaseClient.getConnection();
      stmt = conn.createStatement();

      String sql = "CREATE TABLE episodes " +
          "(id INTEGER not NULL AUTO_INCREMENT, " +
          " game VARCHAR(255), " +
          " level INTEGER, " +
          // GameOptions
          " reuseTree VARCHAR(255), " +
          " lossAvoidance VARCHAR(255), " +
          " expandAllNodes VARCHAR(255), " +
          " safetyPreprunning VARCHAR(255), " +
          " shallowRollout VARCHAR(255), " +
          " totalGames INTEGER, " +
          " scores VARCHAR(255), " +
          " wins INTEGER, " +
          " wr FLOAT, " +
          " PRIMARY KEY ( id ))";

      stmt.executeUpdate(sql);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
    try {
      conn.close();
    } catch (SQLException se) {
      se.printStackTrace();
    }

  }


}
