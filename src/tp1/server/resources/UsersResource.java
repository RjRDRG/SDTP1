package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.jws.WebService;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.soap.SoapUsers;
import tp1.server.WebServiceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static tp1.util.ExceptionMapper.*;

@WebService(
		serviceName = SoapUsers.NAME,
		targetNamespace = SoapUsers.NAMESPACE,
		endpointInterface = SoapUsers.INTERFACE
)
@Singleton
public class UsersResource implements RestUsers, SoapUsers {

	private String domainId;
	private WebServiceType type;

	private final Map<String, User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	public UsersResource(String domainId, WebServiceType type) {
		this.domainId = domainId;
		this.type = type;
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);

		if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null || 
				user.getEmail() == null) {
			throwWebAppException(Log, "User object invalid.", type, Status.BAD_REQUEST );
		}

		synchronized ( this ) {

			String userId = user.getUserId() + "@" + domainId;

			if(users.containsKey(userId)) {
				throwWebAppException(Log, "User already exists.", type, Status.CONFLICT);
			}

			users.put(userId, user);

			return userId;
		}
	}


	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		if(userId == null || password == null) {
			throwWebAppException(Log, "UserId or password null.", type, Status.BAD_REQUEST);
		}

		User user = users.get(userId);

		if( user == null ) {
			throwWebAppException(Log, "User does not exist.", type, Status.NOT_FOUND ); //Nao mudar mensagem de erro
		}

		if(!user.getPassword().equals(password)) {
			throwWebAppException(Log, "Password is incorrect.", type, Status.FORBIDDEN );
		}

		return user;
	}


	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);

		if(userId == null || password == null) {
			throwWebAppException(Log, "UserId or password null.", type, Status.BAD_REQUEST );
		}

		synchronized ( this ) {
			User oldUser = users.get(userId);

			if( oldUser == null ) {
				throwWebAppException(Log, "User does not exist.", type, Status.NOT_FOUND );
			}

			if( !oldUser.getPassword().equals( password)) {
				throwWebAppException(Log, "Password is incorrect.", type, Status.FORBIDDEN );
			}

			users.put(userId, new User(userId, user.getFullName(), user.getEmail(), user.getPassword()));

			return oldUser;
		}
	}


	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);

		if(userId == null || password == null) {
			throwWebAppException(Log, "UserId or password null.", type, Status.BAD_REQUEST );
		}

		synchronized ( this ) {
			User user = users.get(userId);

			if( user == null ) {
				throwWebAppException(Log, "User does not exist.", type, Status.NOT_FOUND );
			}

			if( !user.getPassword().equals( password)) {
				throwWebAppException(Log, "Password is incorrect.", type, Status.FORBIDDEN );
			}


			return users.remove(userId);
		}
	}


	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);

		if(users.isEmpty()) {
			return new ArrayList<>();
		}

		if( pattern == null || pattern.isEmpty() ) {
			return new ArrayList<>(users.values());
		}

		return users.values().stream().filter(u -> u.getFullName().contains(pattern)).collect(Collectors.toList());
	}

}
