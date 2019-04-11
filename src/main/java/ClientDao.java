import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDao implements DAO<Client> {
    Connection connection;
    public ClientDao() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:apiConnect.db3");
    }

    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Client (" +
                "phoneFirst TEXT NOT NULL, " +
                "phoneSecond TEXT, ref TEXT, site TEXT, time TEXT);";
        try {
            DriverManager.registerDriver (new JDBC());
            Statement statement = connection.createStatement();
            int row = statement.executeUpdate(sql);
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println(e);
        }
    }

    @Override
    public void add(Client client) throws SQLException {
        String sql = "INSERT INTO Client (phoneFirst, phoneSecond, ref, site, time) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, client.getPhoneFirst());
        statement.setString(2, client.getPhoneSecond());
        statement.setString(3, client.getRef());
        statement.setString(4, client.getSite());
        statement.setString(5, client.getTime());
        int row = statement.executeUpdate();
    }

    @Override
    public void delete() throws SQLException {
        String sql = "DELETE FROM Client;";
        PreparedStatement statement = null;
        statement = connection.prepareStatement(sql);
        int row = statement.executeUpdate();
    }

    @Override
    public List<Client> getAll() throws SQLException {
        String sql = "SELECT * FROM Client;";
        List<Client> clients = new ArrayList<>();
        PreparedStatement statement = null;
        statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        for(int i = 0; resultSet.next(); i++){
            clients.add(new Client(resultSet.getString("phoneFirst"), resultSet.getString("phoneSecond"),
                    resultSet.getString("ref"), resultSet.getString("site"), resultSet.getString("time")));
        }
        return clients;
    }
}
