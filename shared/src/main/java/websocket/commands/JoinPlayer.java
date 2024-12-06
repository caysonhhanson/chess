package websocket.commands;

public class JoinPlayer extends UserGameCommand {
  private final String playerColor;

  public JoinPlayer(String authToken, Integer gameID, String playerColor) {
    super(CommandType.CONNECT, authToken, gameID);
    this.playerColor = playerColor;
  }

  public String getPlayerColor() {
    return playerColor;
  }
}