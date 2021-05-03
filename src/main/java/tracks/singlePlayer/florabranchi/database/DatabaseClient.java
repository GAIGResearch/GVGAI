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

  private static final String SQL_SERIALIZE_OBJECT = "INSERT INTO %s(version, serialized_object) VALUES (?, ?)";
  private static final String SQL_DESERIALIZE_OBJECT = "SELECT serialized_object FROM %s WHERE id = ?";


  public static void serializeWeights(Connection connection,
                                      final String table,
                                      SavedMetaWeights objectToSerialize) throws SQLException {

    PreparedStatement pstmt = connection
        .prepareStatement(String.format(SQL_SERIALIZE_OBJECT, table));

    // just setting the class name
    pstmt.setObject(1, objectToSerialize.currentVersion);
    pstmt.setObject(2, objectToSerialize);
    pstmt.executeUpdate();
    pstmt.close();
    System.out.println("Java object serialized to database. Object: " + objectToSerialize);
  }

  public static SavedMetaWeights deSerializeWeights(Connection connection,
                                                    final String table,
                                                    long id) throws SQLException, IOException,
      ClassNotFoundException {

    PreparedStatement pstmt = connection.prepareStatement(String.format(SQL_DESERIALIZE_OBJECT, table));
    pstmt.setLong(1, id);
    ResultSet rs = pstmt.executeQuery();
    rs.next();

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
      return (SavedMetaWeights) deSerializedObject;
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
        System.out.println("Connected");
      }

      return conn;
    } catch (SQLException | ClassNotFoundException ex) {
      ex.printStackTrace();
    }

    return null;
  }


  public void connect() {

  }

  public void insert() throws SQLException {

    Connection conn = null;
    conn = getConnection();
    String sql = "INSERT INTO Users (username, password, fullname, email) VALUES (?, ?, ?, ?)";

    PreparedStatement statement = conn.prepareStatement(sql);
    statement.setString(1, "bill");
    statement.setString(2, "secretpass");
    statement.setString(3, "Bill Gates");
    statement.setString(4, "bill.gates@microsoft.com");

    int rowsInserted = statement.executeUpdate();
    if (rowsInserted > 0) {
      System.out.println("A new user was inserted successfully!");
    }
  }


}
