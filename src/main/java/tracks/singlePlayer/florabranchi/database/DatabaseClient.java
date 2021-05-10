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

  private static final String SQL_SERIALIZE_OBJECT = "INSERT INTO %s(serialized_object, iterations, game) VALUES (?, ?, ?)";
  private static final String SQL_DESERIALIZE_OBJECT = "SELECT serialized_object,iterations,game FROM cmab_data WHERE game=?";
  private static final String SQL_UPDATE_OBJECT = "UPDATE cmab_data SET serialized_object = ?, iterations = ? WHERE game=?";


  public static void update(Connection connection,
                            BanditsArmDTO objectToSerialize) throws SQLException {

    objectToSerialize.id = 1;
    objectToSerialize.iterations++;
    PreparedStatement pstmt = connection.prepareStatement(SQL_UPDATE_OBJECT);
    pstmt.setObject(1, objectToSerialize.object);
    pstmt.setObject(2, objectToSerialize.iterations);
    pstmt.setObject(3, objectToSerialize.game);
    pstmt.executeUpdate();
    pstmt.close();
    System.out.println("updated bandit" + objectToSerialize);
  }

  public static void serializeWeights(Connection connection,
                                      final String table,
                                      BanditsArmDTO objectToSerialize) throws SQLException {

    PreparedStatement pstmt = connection
        .prepareStatement(String.format(SQL_SERIALIZE_OBJECT, table));

    objectToSerialize.iterations = 1;
    pstmt.setObject(1, objectToSerialize.object);
    pstmt.setObject(2, objectToSerialize.iterations);
    pstmt.setObject(3, objectToSerialize.game);
    pstmt.executeUpdate();
    pstmt.close();
    System.out.println("Java object serialized to database. Object: " + objectToSerialize);
  }

  public static BanditsArmDTO deSerializeWeights(Connection connection,
                                                 String game) throws SQLException, IOException,
      ClassNotFoundException {

    PreparedStatement pstmt = connection.prepareStatement(SQL_DESERIALIZE_OBJECT);
    pstmt.setString(1, game);
    ResultSet rs = pstmt.executeQuery();

    BanditsArmDTO banditsArmDTO = null;
    Object deSerializedObject = null;
    while (rs.next()) {

      Object object = rs.getObject(1);
      Object iterations = rs.getInt(2);
      Object gameObj = rs.getString(3);

      byte[] buf = rs.getBytes(1);
      ObjectInputStream objectIn = null;
      if (buf != null)
        objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));

      if (objectIn != null) {
        deSerializedObject = objectIn.readObject();

        banditsArmDTO = new BanditsArmDTO();
        banditsArmDTO.iterations = (int) iterations;
        banditsArmDTO.object = (BanditArmsData) deSerializedObject;
        banditsArmDTO.game = (String) gameObj;

        System.out.println("Java object de-serialized from database. Object: "
            + deSerializedObject + " Classname: "
            + deSerializedObject.getClass().getName());
      }
    }

    rs.close();
    pstmt.close();
    return banditsArmDTO;
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
