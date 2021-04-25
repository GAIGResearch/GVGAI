package tracks.singlePlayer.florabranchi.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseClient {

  //
  String dbURL = "jdbc:mysql://localhost:3306/ffbranchi";
  String username = "root";
  String password = "root";

  public DatabaseClient() {
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
