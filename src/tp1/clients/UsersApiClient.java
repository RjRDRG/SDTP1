package tp1.clients;

import tp1.api.User;
import tp1.api.service.soap.SoapException;

import java.util.List;

public interface UsersApiClient {

    String SERVICE = "UsersService";

    String createUser(User user);

    Boolean verifyUser (String userId, String password);

    User getUser(String userId, String password);

    User updateUser(String userId, String password, User user);

    User deleteUser(String userId, String password);

    List<User> searchUsers(String pattern);
}
