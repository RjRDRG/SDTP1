package tp1.clients;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.server.resources.UsersResource;

import java.util.List;
import java.util.logging.Logger;

public class UsersRestClient implements UsersApiClient {

    private static Logger Log = Logger.getLogger(UsersResource.class.getName());

    private final WebTarget target;

    public UsersRestClient(String serverUrl) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        target = client.target(serverUrl).path( RestUsers.PATH );
    }

    @Override
    public String createUser(User user)  {
        Response r = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(user, MediaType.APPLICATION_JSON));

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(String.class);
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public Boolean verifyUser(String userId, String password) {

        if( password == null) {
            return false;
        }

        Response r = target.path(userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        
        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() ) {
            return true;
        }
        if( r.getStatus() == Response.Status.FORBIDDEN.getStatusCode() ) {
            return false;
        }
        else {
            throw new WebApplicationException(r.getStatus());
        }

    }

    @Override
    public User getUser(String userId, String password)  {

        Response r = target.path(userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(User.class);
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public User updateUser(String userId, String password, User user)  {

        Response r = target.path(userId).queryParam("password",  password).request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(user, MediaType.APPLICATION_JSON));

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(User.class);
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public User deleteUser(String userId, String password)  {
        Response r = target.path( userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(User.class);
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public List<User> searchUsers(String pattern)  {
        Response r = target.path("/").queryParam("query", pattern).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() ) {
            return r.readEntity(new GenericType<List<User>>() {});
        } else
            throw new WebApplicationException(r.getStatus());
    }
}
