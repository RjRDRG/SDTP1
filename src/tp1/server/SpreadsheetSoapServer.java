package tp1.server;

import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.Endpoint;
import tp1.discovery.Discovery;
import tp1.server.resources.SpreadsheetResource;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static tp1.clients.SpreadsheetApiClient.SERVICE;

public class SpreadsheetSoapServer {

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SOAP_USERS_PATH = "/soap/spreadsheets";

    public static void main(String[] args) {
        try {
            String domain = args.length > 1 ? args[0] : "OutdatedPieceOfSht";

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format("http://%s:%s/soap", ip, PORT);


            HttpServer server = HttpServer.create(new InetSocketAddress(ip, PORT), 0);

            server.setExecutor(Executors.newCachedThreadPool());
            Endpoint soapUsersEndpoint = Endpoint.create(new SpreadsheetResource(domain, WebServiceType.SOAP));
            soapUsersEndpoint.publish(server.createContext(SOAP_USERS_PATH));
            server.start();

            Discovery discovery = new Discovery( domain, SERVICE, serverURI);
            SpreadsheetResource.setDiscovery(discovery);
            discovery.startSendingAnnouncements();
            discovery.startCollectingAnnouncements();

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
