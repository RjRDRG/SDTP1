package tp1.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.discovery.Discovery;
import tp1.server.WebServiceType;
import tp1.resources.UsersResource;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import static tp1.clients.user.UsersClient.SERVICE;

public class UsersRestServer {

	private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 8080;
	
	public static void main(String[] args) {
		try {
			String domain = args.length > 0 ? args[0] : "afasfasfafasf";

			String ip = InetAddress.getLocalHost().getHostAddress();

			String serverURI = String.format("http://%s:%s/rest", ip, PORT);

			ResourceConfig config = new ResourceConfig();
			config.register(new UsersResource(domain, WebServiceType.REST));

			JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

			Discovery discovery = new Discovery( domain, SERVICE ,serverURI);

			UsersResource.setDiscovery(discovery);
			discovery.startSendingAnnouncements();
			discovery.startCollectingAnnouncements();

			Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}
	
}