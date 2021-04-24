package tp1.clients;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

public class UsersSoapClient implements UsersApiClient {

    public final static String USERS_WSDL = "/users/?wsdl";

    public final static int MAX_RETRIES = 5;
    public final static long RETRY_PERIOD = 1000;
    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    public final SoapUsers target;

    public UsersSoapClient(String serverUrl) throws MalformedURLException {
        QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
        Service service = Service.create( new URL(serverUrl + USERS_WSDL), QNAME );
        target = service.getPort( SoapUsers.class );

        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws UsersException;
    }

    private <T> T retry(CheckedSupplier<T> supplier ) throws UsersException {
        UsersException exception;

        int retries=0;
        do {
            retries++;

            try {
                return supplier.get();
            } catch (UsersException e) {
                exception = e;
                break;
            } catch (Exception e) {
                exception = new UsersException(e.getMessage());
            }

            try { Thread.sleep(RETRY_PERIOD); } catch (InterruptedException ignored) {}

        } while (retries < MAX_RETRIES);

        throw exception;
    }

    @Override
    public String createUser(User user) throws UsersException {
        return retry( () -> target.createUser(user) );
    }

    @Override
    public Boolean verifyUser(String userId, String password) throws UsersException {
        return retry( () -> {
            try {
                target.getUser(userId, password);
                return true;
            } catch (Exception exception) {
                if(exception.getMessage().contains("not exist"))
                    return false;
                else
                    throw exception;
            }
        });
    }

    @Override
    public User getUser(String userId, String password) throws UsersException {
        return retry( () -> target.getUser(userId, password) );
    }

    @Override
    public User updateUser(String userId, String password, User user) throws UsersException {
        return retry( () -> target.updateUser(userId, password, user) );
    }

    @Override
    public User deleteUser(String userId, String password) throws UsersException {
        return retry( () -> target.deleteUser(userId, password) );
    }

    @Override
    public List<User> searchUsers(String pattern) throws UsersException {
        return retry( () -> target.searchUsers(pattern) );
    }
}
