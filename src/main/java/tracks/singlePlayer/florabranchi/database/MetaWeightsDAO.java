package tracks.singlePlayer.florabranchi.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import tracks.singlePlayer.florabranchi.agents.meta.MetaWeights;

public class MetaWeightsDAO {

  DatabaseClient databaseClient;

  public MetaWeightsDAO(final DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  public void save(SavedMetaWeights results) {

    String sql = "INSERT INTO meta_weights(version,weights)" + " VALUES(?,?)";

    Connection conn = null;
    PreparedStatement pstm = null;

    try {
      //Cria uma conexão com o banco
      conn = databaseClient.getConnection();
      DatabaseClient.serializeWeights(conn, "meta_weights", results);
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

  public MetaWeights getMetaWeights(final long id) {

    final Statement stmt;
    Connection conn = null;
    try {
      conn = databaseClient.getConnection();

      DatabaseClient.deSerializeWeights(conn, "meta_weights", id);

    } catch (SQLException | IOException | ClassNotFoundException throwables) {
      throwables.printStackTrace();
    }
    try {
      conn.close();
    } catch (SQLException se) {
      se.printStackTrace();
    }

    return new MetaWeights();


  }

  public void createMetaWeightsTable() {

    final Statement stmt;
    Connection conn = null;
    try {
      conn = databaseClient.getConnection();
      stmt = conn.createStatement();

      String sql = "CREATE TABLE meta_weights " +
          "(id INTEGER not NULL AUTO_INCREMENT, " +
          " version INTEGER, " +
          " serialized_object BLOB, " +
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
