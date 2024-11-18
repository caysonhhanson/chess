package service;

import dataaccess.*;
import model.*;
import chess.ChessGame;
import java.util.Collection;

public class GameService {
  private final UserDAO userDAO;
  private final GameDAO gameDAO;
  private final AuthDAO authDAO;

  public GameService(UserDAO userDAO, GameDAO gameDAO, AuthDAO authDAO) {
    this.userDAO = userDAO;
    this.gameDAO = gameDAO;
    this.authDAO = authDAO;
  }

  public void clear() throws DataAccessException {
    userDAO.clear();
    gameDAO.clear();
    authDAO.clear();
  }

  public Collection<GameData> listGames(String authToken) throws DataAccessException, UnauthorizedException {
    AuthData auth = authDAO.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }
    return gameDAO.listGames();
  }

  public CreateGameResult createGame(String authToken, String gameName)
          throws DataAccessException, UnauthorizedException, BadRequestException {
    if (gameName == null || gameName.isBlank()) {
      throw new BadRequestException("Error: bad request");
    }

    AuthData auth = authDAO.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }

    GameData newGame = new GameData(0, null, null, gameName, new ChessGame());
    gameDAO.createGame(newGame);

    Collection<GameData> games = gameDAO.listGames();
    GameData createdGame = games.stream()
            .filter(g -> g.gameName().equals(gameName))
            .findFirst()
            .orElseThrow(() -> new DataAccessException("Failed to create game"));

    return new CreateGameResult(createdGame.gameID());
  }

  public record CreateGameResult(int gameID) {}

  public void joinGame(String authToken, String playerColor, int gameID)
          throws DataAccessException, UnauthorizedException, BadRequestException, AlreadyTakenException {

    AuthData auth = authDAO.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }

    GameData game;
    try {
      game = gameDAO.getGame(gameID);
      if (game == null) {
        throw new BadRequestException("Error: game not found");
      }
    } catch (DataAccessException | BadRequestException e) {
      throw new BadRequestException("Error: game not found");
    }

    if (playerColor == null) {
      return;
    }

    switch (playerColor.toUpperCase()) {
      case "WHITE" -> {
        if (game.whiteUsername() != null && !game.whiteUsername().equals(auth.username())) {
          throw new AlreadyTakenException("Error: already taken");
        }
        game = new GameData(game.gameID(), auth.username(), game.blackUsername(),
                game.gameName(), game.game());
      }
      case "BLACK" -> {
        if (game.blackUsername() != null && !game.blackUsername().equals(auth.username())) {
          throw new AlreadyTakenException("Error: already taken");
        }
        game = new GameData(game.gameID(), game.whiteUsername(), auth.username(),
                game.gameName(), game.game());
      }
      default -> throw new BadRequestException("Error: bad request");
    }

    gameDAO.updateGame(game);
  }
}
