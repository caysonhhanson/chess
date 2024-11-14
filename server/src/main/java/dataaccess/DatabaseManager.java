package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static String databaseName;
    private static final String USER;
    private static final String PASSWORD;
    private static final String CONNECTION_URL;

    /*
     * Load the database information for the db.properties file.
     */
    static {
        try {
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (propStream == null) {
                    throw new Exception("Unable to load db.properties");
                }
                Properties props = new Properties();
                props.load(propStream);
                databaseName = props.getProperty("db.name");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password");

                var host = props.getProperty("db.host");
                var port = Integer.parseInt(props.getProperty("db.port"));
                CONNECTION_URL = String.format("jdbc:mysql://%s:%d", host, port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties. " + ex.getMessage());
        }
    }

    public static void setDatabaseName(String name) {
        databaseName = name;
    }

    public static String getDatabaseName() {
        return databaseName;
    }

    public static void createDatabase() throws DataAccessException {
        try {
            var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            try (var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
                 var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    public static Connection getConnection() throws DataAccessException {
        try {
            createDatabase();
            var conn = DriverManager.getConnection(CONNECTION_URL + "/" + databaseName, USER, PASSWORD);
            conn.setCatalog(databaseName);
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    public static void clearDatabase() throws DataAccessException {
        try (var conn = getConnection()) {
            var clearStatements = new String[] {
                    "SET FOREIGN_KEY_CHECKS = 0;",
                    "TRUNCATE TABLE auth_tokens;",
                    "TRUNCATE TABLE games;",
                    "TRUNCATE TABLE users;",
                    "SET FOREIGN_KEY_CHECKS = 1;"
            };

            try (var stmt = conn.createStatement()) {
                for (var statement : clearStatements) {
                    stmt.executeUpdate(statement);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to clear database: " + ex.getMessage());
        }
    }
}
