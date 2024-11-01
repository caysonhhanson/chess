package dataaccess;

import model.AuthData;

import java.util.HashMap;
import java.util.Map;

public class MemoryAuthDAO implements AuthDAO {
  private final Map<String, AuthData> auths = new HashMap<>();

  @Override
  public void createAuth(AuthData auth) throws DataAccessException {
    auths.put(auth.authToken(), auth);
  }

  @Override
  public AuthData getAuth(String authToken) throws DataAccessException {
    return auths.get(authToken);
  }

  @Override
  public void deleteAuth(String authToken) throws DataAccessException {
    auths.remove(authToken);
  }

  @Override
  public void clear() throws DataAccessException {
    auths.clear();
  }
}

