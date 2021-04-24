package tp1.clients;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.soap.UsersException;
import tp1.server.resources.UsersResource;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class UsersRestClient implements UsersApiClient {

    private static Logger Log = Logger.getLogger(UsersResource.class.getName());

    public final static int MAX_RETRIES = 5;
    public final static long RETRY_PERIOD = 1000;
    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    private final WebTarget target;

    public UsersRestClient(String serverUrl) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        client.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        client.property(ClientProperties.READ_TIMEOUT,    REPLY_TIMEOUT);
        target = client.target(serverUrl).path( RestUsers.PATH );
    }

    private <T> T retry(Supplier<T> supplier ) {
        RuntimeException exception;

        int retries=0;
        do {
            retries++;

            try {
                return supplier.get();
            } catch (ProcessingException e) {
                exception = e;
            }

            try { Thread.sleep(RETRY_PERIOD); } catch (InterruptedException ignored) {}

        } while (retries < MAX_RETRIES);

        throw exception;
    }

    @Override
    public String createUser(User user)  {
        return retry( () -> {
            Response r = target.request()
                    .accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(user, MediaType.APPLICATION_JSON));

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return r.readEntity(String.class);
            else
                throw new WebApplicationException(r.getStatus());
        });
    }

    @Override
    public Boolean verifyUser(String userId, String password) {
        return retry( () -> {
            if (password == null) {
                return false;
            }

            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
                return true;
            }
            if (r.getStatus() == Response.Status.FORBIDDEN.getStatusCode()) {
                return false;
            } else {
                throw new WebApplicationException(r.getStatus());
            }
        });
    }

    @Override
    public User getUser(String userId, String password)  {
        return retry( () -> {
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return r.readEntity(User.class);
            else
                throw new WebApplicationException(r.getStatus());
        });
    }

    @Override
    public User updateUser(String userId, String password, User user)  {
        return retry( () -> {
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .put(Entity.entity(user, MediaType.APPLICATION_JSON));

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return r.readEntity(User.class);
            else
                throw new WebApplicationException(r.getStatus());
        });
    }

    @Override
    public User deleteUser(String userId, String password)  {
        return retry( () -> {
            Response r = target.path(userId).queryParam("password", password).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
                return r.readEntity(User.class);
            else
                throw new WebApplicationException(r.getStatus());
        });
    }

    @Override
    public List<User> searchUsers(String pattern)  {
        return retry( () -> {
            Response r = target.path("/").queryParam("query", pattern).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            if (r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity()) {
                return r.readEntity(new GenericType<List<User>>() {
                });
            } else
                throw new WebApplicationException(r.getStatus());
        });
    }
}
