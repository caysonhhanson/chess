package service;

import dataaccess.*;
import model.*;
import chess.ChessGame;
import java.util.Collection;

public class GameService {
  private final DataAccess dataAccess;

  public GameService(DataAccess dataAccess) {
    this.dataAccess=dataAccess;
  }

  public void clear() throws DataAccessException {
    dataAccess.clear();
  }
}