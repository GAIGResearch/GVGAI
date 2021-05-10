package tracks.singlePlayer.florabranchi.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.persistence.EntityManagerFactory;

public class BanditArmsDataDAO {

  public BanditArmsDataDAO() {
    databaseClient = new DatabaseClient();
  }

  DatabaseClient databaseClient;

  public BanditArmsDataDAO(final DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  public void updateBandit(BanditsArmDTO results) {
    Connection conn = null;
    try {
      conn = databaseClient.getConnection();
      DatabaseClient.update(conn, results);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void saveBandit(BanditsArmDTO results) {

    Connection conn = null;
    PreparedStatement pstm = null;
    try {
      conn = databaseClient.getConnection();
      DatabaseClient.serializeWeights(conn, "cmab_data", results);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

      try {
        if (conn != null) {
          conn.close();
        }
      } catch (Exception e) {

        e.printStackTrace();
      }
    }
  }

  public BanditsArmDTO getBanditArmsDataForGame(final String game) {

    Connection conn = null;
    try {
      conn = databaseClient.getConnection();
      return DatabaseClient.deSerializeWeights(conn, game);
    } catch (SQLException | IOException | ClassNotFoundException throwables) {
      throwables.printStackTrace();
    }
    try {
      conn.close();
    } catch (SQLException se) {
      se.printStackTrace();
    }
    return null;
  }

  public void createMetaWeightsTable() {

    final Statement stmt;
    Connection conn = null;
    try {
      conn = databaseClient.getConnection();
      stmt = conn.createStatement();

      String sql = "CREATE TABLE cmab_data " +
          "(id INTEGER not NULL AUTO_INCREMENT, " +
          " serialized_object BLOB, " +
          " iterations INTEGER, " +
          " game VARCHAR(255), " +
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
