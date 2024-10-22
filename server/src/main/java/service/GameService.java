package service;

import dataaccess.*;
import model.*;
import chess.ChessGame;
import java.util.Collection;

public class GameService {
  private final DataAccess dataAccess;

  public GameService(DataAccess dataAccess) {
    this.dataAccess = dataAccess;
  }

  public void clear() throws DataAccessException {
    dataAccess.clear();
  }

  public Collection<GameData> listGames(String authToken) throws DataAccessException, UnauthorizedException {
    AuthData auth = dataAccess.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }
    return dataAccess.listGames();
  }

  public CreateGameResult createGame(String authToken, String gameName)
          throws DataAccessException, UnauthorizedException, BadRequestException {
    // Validate inputs
    if (gameName == null || gameName.isBlank()) {
      throw new BadRequestException("Error: bad request");
    }

    AuthData auth = dataAccess.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }

    GameData newGame = new GameData(0, null, null, gameName, new ChessGame());
    dataAccess.createGame(newGame);

    // Get the game to get its assigned ID
    Collection<GameData> games = dataAccess.listGames();
    GameData createdGame = games.stream()
            .filter(g -> g.gameName().equals(gameName))
            .findFirst()
            .orElseThrow(() -> new DataAccessException("Failed to create game"));

    return new CreateGameResult(createdGame.gameID());
  }

  public record CreateGameResult(int gameID) {}
  public void joinGame(String authToken, String playerColor, int gameID)
          throws DataAccessException, UnauthorizedException, BadRequestException, AlreadyTakenException {
    // Verify auth token
    AuthData auth = dataAccess.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }

    // Verify game exists
    GameData game = dataAccess.getGame(gameID);
    if (game == null) {
      throw new BadRequestException("Error: bad request");
    }

    // Handle spectator case (null playerColor means watch only)
    if (playerColor == null) {
      return; // Successfully joined as spectator
    }

    // Validate and process color selection
    switch (playerColor.toUpperCase()) {
      case "WHITE" -> {
        if (game.whiteUsername() != null) {
          throw new AlreadyTakenException("Error: already taken");
        }
        game = new GameData(game.gameID(), auth.username(), game.blackUsername(),
                game.gameName(), game.game());
      }
      case "BLACK" -> {
        if (game.blackUsername() != null) {
          throw new AlreadyTakenException("Error: already taken");
        }
        game = new GameData(game.gameID(), game.whiteUsername(), auth.username(),
                game.gameName(), game.game());
      }
      default -> throw new BadRequestException("Error: bad request");
    }

    // Update the game in the database
    dataAccess.updateGame(game);
  }
}
