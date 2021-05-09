package tracks.singlePlayer.florabranchi.database;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DatabaseClient {

  //
  String dbURL = "jdbc:mysql://localhost:3306/ffbranchi";
  String username = "root";
  String password = "root";

  public DatabaseClient() {
  }

  private static final String SQL_SERIALIZE_OBJECT = "INSERT INTO %s(serialized_object) VALUES (?)";
  private static final String SQL_DESERIALIZE_OBJECT = "SELECT serialized_object FROM cmab_data WHERE id=?";
  private static final String SQL_UPDATE_OBJECT = "UPDATE cmab_data SET serialized_object = ? WHERE id=1";


  public static void update(Connection connection,
                            BanditsArmDTO objectToSerialize) throws SQLException {

    PreparedStatement pstmt = connection.prepareStatement(SQL_UPDATE_OBJECT);
    pstmt.setObject(1, objectToSerialize.object);
    pstmt.executeUpdate();
    pstmt.close();
    System.out.println("updated bandit" + objectToSerialize);
  }

  public static void serializeWeights(Connection connection,
                                      final String table,
                                      BanditsArmDTO objectToSerialize) throws SQLException {

    PreparedStatement pstmt = connection
        .prepareStatement(String.format(SQL_SERIALIZE_OBJECT, table));

    pstmt.setObject(1, objectToSerialize.object);
    pstmt.executeUpdate();
    pstmt.close();
    System.out.println("Java object serialized to database. Object: " + objectToSerialize);
  }

  public static BanditArmsData deSerializeWeights(Connection connection,
                                                  final String table,
                                                  int id) throws SQLException, IOException,
      ClassNotFoundException {

    PreparedStatement pstmt = connection.prepareStatement(SQL_DESERIALIZE_OBJECT);
    pstmt.setInt(1, id);
    ResultSet rs = pstmt.executeQuery();

    if (rs.next()) {

      Object object = rs.getObject(1);

      byte[] buf = rs.getBytes(1);
      ObjectInputStream objectIn = null;
      if (buf != null)
        objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));

      Object deSerializedObject = objectIn.readObject();

      System.out.println("Java object de-serialized from database. Object: "
          + deSerializedObject + " Classname: "
          + deSerializedObject.getClass().getName());
      return (BanditArmsData) deSerializedObject;
    }


    rs.close();
    pstmt.close();

    return null;
  }


  public Connection getConnection() {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      Connection conn = DriverManager.getConnection(dbURL, username, password);
      if (conn != null) {
        //System.out.println("Connected");
      }

      return conn;
    } catch (SQLException | ClassNotFoundException ex) {
      ex.printStackTrace();
    }

    return null;
  }

}
