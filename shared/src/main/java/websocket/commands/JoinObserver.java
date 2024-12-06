package websocket.commands;

public class JoinObserver extends UserGameCommand {
  public JoinObserver(String authToken, Integer gameID) {
    super(CommandType.CONNECT, authToken, gameID);
  }
}