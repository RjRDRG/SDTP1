package tp1.api.service.soap;

import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import tp1.api.User;

@WebService(serviceName=SoapUsers.NAME, targetNamespace=SoapUsers.NAMESPACE, endpointInterface=SoapUsers.INTERFACE)
public interface SoapUsers {

	static final String NAME = "users";
	static final String NAMESPACE = "http://sd2021";
	static final String INTERFACE = "tp1.api.service.soap.SoapUsers";

	@WebMethod
	String createUser(User user) throws SoapException;
	
	/**
	 * Obtains the information on the user identified by name.
	 * @param userId the userId of the user
	 * @param password password of the user
	 * @throws SoapException otherwise
	 */
	@WebMethod
	User getUser(String userId, String password) throws SoapException;
	
	/**
	 * Modifies the information of a user. Values of null in any field of the user will be 
	 * considered as if the the fields is not to be modified (the id cannot be modified).
	 * @param userId the userId of the user
	 * @param password password of the user
	 * @param user Updated information
	 * @throws SoapException otherwise
	 */
	@WebMethod
	User updateUser(String userId, String password, User user) throws SoapException;
	
	/**
	 * Deletes the user identified by userId. The spreadsheets owned by the user should be eventually removed (asynchronous
	 * deletion is ok).
	 * @param nauserId the userId of the user
	 * @param password password of the user
	 * @throws SoapException otherwise
	 */
	@WebMethod
	User deleteUser(String userId, String password) throws SoapException;
	
	/**
	 * Returns the list of users for which the pattern is a substring of the name (of the user), case-insensitive.
	 * The password of the users returned by the query must be set to the empty string "".
	 * @param pattern substring to search
	 * @throws SoapException otherwise
	 */
	@WebMethod
	List<User> searchUsers(String pattern) throws SoapException;
}
