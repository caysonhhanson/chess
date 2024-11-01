package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.*;
import org.mindrot.jbcrypt.BCrypt; // Use jBCrypt instead of Spring's BCrypt
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class MySQLDataAccess implements DataAccess {
  private final Gson gson = new Gson();

  public MySQLDataAccess() {
    try {
      configureDatabase();
    } catch (DataAccessException e) {
      throw new RuntimeException("Failed to configure database: " + e.getMessage());
    }
  }
