package tracks.singlePlayer.florabranchi.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


public class BanditArmsDataDAO {

  static EntityManagerFactory entityManagerFactory = null;

  public BanditArmsDataDAO() {
    if (entityManagerFactory == null) {
      //entityManagerFactory = Persistence.createEntityManagerFactory("cmab_data");
    }

    databaseClient = new DatabaseClient();
  }

  DatabaseClient databaseClient;

  public BanditArmsDataDAO(final DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  public void saveBandit(BanditsArmDTO results) {


    Connection conn = null;
    PreparedStatement pstm = null;

    try {
      //Cria uma conexão com o banco
      conn = databaseClient.getConnection();
      DatabaseClient.serializeWeights(conn, "cmab_data", results);
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

  public BanditsArmDTO getMetaWeights(final int id) {

    final Statement stmt;
    Connection conn = null;
    try {
      conn = databaseClient.getConnection();

      return DatabaseClient.deSerializeWeights(conn, "cmab_data", id);


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
