import chess.*;
import client.ServerFacade;
import ui.PreLoginREPL;

public class Main {
  public static void main(String[] args) throws Exception {
    System.out.println("240 Chess Client:");
    int port = 0;
    ServerFacade server = new ServerFacade(port);
    PreLoginREPL prelogin = new PreLoginREPL(server);
    prelogin.run();
    System.out.println("Ended");

  }
}
