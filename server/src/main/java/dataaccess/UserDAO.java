package dataaccess;

import model.UserData;
import java.util.Collection;

public interface UserDAO {
  void createUser(UserData user) throws DataAccessException;
  UserData getUser(String username) throws DataAccessException;
  Collection<UserData> listUsers() throws DataAccessException;
  void clear() throws DataAccessException;
}