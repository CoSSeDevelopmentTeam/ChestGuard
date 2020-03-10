package net.comorevi.cosse.chestguard.util;

import java.sql.*;

public class SQLite3DataProvider {

    private Connection connection = null;

    public SQLite3DataProvider() {
        connectSQL();
    }

    public boolean existsChestData(String formattedLocation) {
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

    public void createChestData(String formattedLocation, String owner) {
        try {
            if (existsChestData(formattedLocation)) return;

            String sql = "INSERT INTO chestguard ( owner, location, type, option ) values ( ?, ?, ?, ? )";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setQueryTimeout(30);
            statement.setString(1, owner);
            statement.setString(2, formattedLocation);
            statement.setInt(3, ProtectType.PROTECT_TYPE_DEFAULT.getId());
            statement.setString(4, null);

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteChestData(String formattedLocation) {
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

    public void changeGuardType(String formattedLocation, ProtectType type, String data) {
        String sql = "UPDATE chestguard SET type = ?, option = ? WHERE location = ?";
        switch (type) {
            case PROTECT_TYPE_DEFAULT:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, ProtectType.PROTECT_TYPE_DEFAULT.getId());
                    statement.setString(2, null);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case PROTECT_TYPE_PASSWORD:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, ProtectType.PROTECT_TYPE_PASSWORD.getId());
                    statement.setString(2, data);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case PROTECT_TYPE_SHARE:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, ProtectType.PROTECT_TYPE_SHARE.getId());
                    statement.setString(2, data);
                    statement.setString(3, formattedLocation);

                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            case PROTECT_TYPE_PUBLIC:
                try {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(30);
                    statement.setInt(1, ProtectType.PROTECT_TYPE_PUBLIC.getId());
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

    public String getOwnerName(String formattedLocation) {
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

    public String getGuardType(String formattedLocation) {
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

    public String getOptionData(String formattedLocation) {
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
            connection = DriverManager.getConnection("jdbc:sqlite:./plugins/ChestGuard/DataDB.db");
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

    public void disConnectSQL() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

}
