package tp1.clients;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.clients.SpreadsheetApiClient;
import tp1.server.rest.UsersRestServer;

import java.util.logging.Logger;

public class SpreadsheetRestClient implements SpreadsheetApiClient {

    private final WebTarget target;

    private static Logger Log = Logger.getLogger(SpreadsheetRestClient.class.getName());

    public SpreadsheetRestClient(String serverUrl) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        target = client.target(serverUrl).path( RestSpreadsheets.PATH );
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws WebApplicationException  {

        Response r = target.queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(sheet, MediaType.APPLICATION_JSON));

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(String.class);
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws WebApplicationException {
        Response r = target.path(sheetId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        if( r.getStatus() != Response.Status.OK.getStatusCode() )
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException {

        Response r = target.path(sheetId).queryParam("userId", userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(Spreadsheet.class);
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws WebApplicationException {

        Response r = target.path(sheetId).queryParam("userId", userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(new GenericType<String[][]>() {});
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range) throws WebApplicationException  {

        Response r = target.path(sheetId).queryParam("userId", userId).queryParam("range",range).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if( r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity() )
            return r.readEntity(new GenericType<String[][]>() {});
        else
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) throws WebApplicationException  {

        Response r = target.path(sheetId).path(cell).queryParam("userId", userId).queryParam("password",  password).request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(cell, MediaType.APPLICATION_JSON));

        if( r.getStatus() != Response.Status.OK.getStatusCode() )
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException  {
        Response r = target.path(sheetId).path(userId).queryParam("password",  password).request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(userId, MediaType.APPLICATION_JSON));

        if( r.getStatus() != Response.Status.OK.getStatusCode() && r.hasEntity() )
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password) throws WebApplicationException  {
        Response r = target.path(sheetId).path(userId).queryParam("password",  password).request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        if( r.getStatus() != Response.Status.OK.getStatusCode() && r.hasEntity() )
            throw new WebApplicationException(r.getStatus());
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) {
        Response r = target.path("spreadsheets").path(userId).queryParam("password", password).request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        if( r.getStatus() != Response.Status.OK.getStatusCode() )
            throw new WebApplicationException(r.getStatus());
    }
}
