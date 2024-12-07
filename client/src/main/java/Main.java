import client.ServerFacade;
import ui.PreLoginREPL;

public class Main {
  private static final int DEFAULT_PORT = 8080;

  public static void main(String[] args) throws Exception {
    System.out.println("240 Chess Clients:");

    int port = DEFAULT_PORT;
    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.out.println("Invalid port number. Using default port " + DEFAULT_PORT);
      }
    }

    ServerFacade server = new ServerFacade(port);
    PreLoginREPL prelogin = new PreLoginREPL(server);
    prelogin.run();
    System.out.println("Ended");
  }
}
