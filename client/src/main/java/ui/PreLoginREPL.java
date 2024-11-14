package ui;

import client.ServerFacade;
import static ui.EscapeSequences.*;
import java.util.*;

public class PreLoginREPL {
  private final ServerFacade facade;
  private final  ui.PostLoginREPL postLogin;

  public PreLoginREPL(ServerFacade facade) {
    this.facade = facade;
    this.postLogin = new ui.PostLoginREPL(facade);
  }

  public void run() {
    var scanner = new Scanner(System.in);
    System.out.println(RESET_TEXT_COLOR + RESET_BG_COLOR + "Chess Game - Type 'help' for commands");

    while (true) {
      System.out.print("\n[OUT] >> ");
      String[] args = scanner.nextLine().toLowerCase().split(" ");

      if (args[0].equals("quit")) break;

      if (processCommand(args)) {
        postLogin.run();
        break;
      }
    }
  }

  private boolean processCommand(String[] args) {
    return switch (args[0]) {
      case "help" -> { printCommands(); yield false; }
      case "login" -> handleLogin(args);
      case "register" -> handleRegistration(args);
      default -> { System.out.println("Unknown command - try 'help'"); yield false; }
    };
  }

  private boolean handleLogin(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage: login <username> <password>");
      return false;
    }
    if (facade.login(args[1], args[2])) {
      System.out.println("Login successful");
      return true;
    }
    System.out.println("Login failed");
    return false;
  }

  private boolean handleRegistration(String[] args) {
    if (args.length != 4) {
      System.out.println("Usage: register <username> <password> <email>");
      return false;
    }
    if (facade.register(args[1], args[2], args[3])) {
      System.out.println("Registration successful");
      return true;
    }
    System.out.println("Registration failed");
    return false;
  }

  private void printCommands() {
    System.out.println("""
            Available commands:
            login <username> <password>
            register <username> <password> <email>
            help
            quit""");
  }
}
