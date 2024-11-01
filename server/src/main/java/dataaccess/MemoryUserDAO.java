package dataaccess;

import model.UserData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MemoryUserDAO implements UserDAO {
  private final Map<String, UserData> users = new HashMap<>();

  @Override
  public void createUser(UserData user) throws DataAccessException {
    if (users.containsKey(user.username())) {
      throw new DataAccessException("Error: already taken");
    }
    users.put(user.username(), user);
  }

  @Override
  public UserData getUser(String username) throws DataAccessException {
    return users.get(username);
  }

  @Override
  public Collection<UserData> listUsers() throws DataAccessException {
    return users.values();
  }

  @Override
  public void clear() throws DataAccessException {
    users.clear();
  }
}

