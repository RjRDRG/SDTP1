package tp1.clients;

import tp1.api.User;
import tp1.api.service.soap.UsersException;

import java.util.List;

public interface UsersApiClient {

    String SERVICE = "users";

    String createUser(User user) throws UsersException;

    Boolean verifyUser (String userId, String password) throws UsersException;

    User getUser(String userId, String password) throws UsersException;

    User updateUser(String userId, String password, User user) throws UsersException;

    User deleteUser(String userId, String password) throws UsersException;

    List<User> searchUsers(String pattern) throws UsersException;
}
