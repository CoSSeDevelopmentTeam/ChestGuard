package net.comorevi.cosse.chestguard;

import java.sql.*;

public class SQLite3DataProvider {

    private ChestGuard plugin;
    private Connection connection = null;

    public SQLite3DataProvider(ChestGuard plugin) {
        this.plugin = plugin;
        connectSQL();
    }

    boolean existsChestData(String formattedLocation) {
        try {
            String sql = "SELECT location FROM chestguard WHERE location = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, formattedLocation);

            boolean result = statement.executeQuery().next();
            statement.close();

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    void createChestData(String formattedLocation, String owner) {
        try {
            if (existsChestData(formattedLocation)) return;

            String sql = "INSERT INTO chestguard ( owner, location, type, option ) values ( ?, ?, ?, ? )";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, owner);
            statement.setString(2, formattedLocation);
            statement.setInt(3, GuardType.DEFAULT);
            statement.setString(4, null);

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void deleteChestData(String formattedLocation) {
        try {
            if (!existsChestData(formattedLocation)) return;

            String sql = "DELETE FROM chestguard WHERE location = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, formattedLocation);

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void changeGuardType(String formattedLocation, int type, String data) {
        String sql = "UPDATE chestguard SET ( type , option ) VALUES ( ?, ? ) WHERE location = ?";
        switch (type) {
            case GuardType.DEFAULT:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, GuardType.DEFAULT);
                    statement.setString(2, null);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case GuardType.PASSWORD:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, GuardType.PASSWORD);
                    statement.setString(2, data);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case GuardType.SHARE:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, GuardType.SHARE);
                    statement.setString(2, data);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case GuardType.PUBLIC:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, GuardType.PUBLIC);
                    statement.setString(2, null);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
                default:
                    break;
        }
    }

    String getChestOwnerName(String formattedLocation) {
        try {
            if (!existsChestData(formattedLocation)) return null;

            String sql = "SELECT owner FROM chestguard WHERE location = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, formattedLocation);

            String result = statement.executeQuery().getString("owner");
            statement.close();

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    String getGuardType(String formattedLocation) {
        try {
            if (!existsChestData(formattedLocation)) return null;

            String sql = "SELECT type FROM chestguard WHERE location = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, formattedLocation);

            String result = statement.executeQuery().getString("type");
            statement.close();

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    String getOptionData(String formattedLocation) {
        try {
            if (!existsChestData(formattedLocation)) return null;

            String sql = "SELECT option FROM chestguard WHERE location = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, formattedLocation);

            String result = statement.executeQuery().getString("option");
            statement.close();

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void connectSQL() {
        try {
            Class.forName("org.sqlite.JDBC");
        }catch(Exception e){
            System.err.println(e.getMessage());
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().toString() + "/DataDB.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS chestguard (" +
                            " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            " owner TEXT NOT NULL," +
                            " location TEXT NOT NULL," +
                            " type INTEGER," +
                            " option TEXT)"
            );
            statement.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    void disConnectSQL() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
