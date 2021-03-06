package tp1.discovery;

import tp1.clients.sheet.SpreadsheetCachedClient;
import tp1.clients.sheet.SpreadsheetClient;
import tp1.clients.user.UsersCachedClient;
import tp1.clients.user.UsersClient;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * <p>A class to perform service discovery, based on periodic service contact endpoint 
 * announcements over multicast communication.</p>
 * 
 * <p>Servers announce their *name* and contact *uri* at regular intervals. The server actively
 * collects received announcements.</p>
 * 
 * <p>Service announcements have the following format:</p>
 * 
 * <p>&lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;</p>
 */
public class Discovery {
	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	static {
		// addresses some multicast issues on some TCP/IP stacks
		System.setProperty("java.net.preferIPv4Stack", "true");
		// summarizes the logging format
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	
	// The pre-aggreed multicast endpoint assigned to perform discovery. 
	private static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	private static final int DISCOVERY_PERIOD = 1000;

	private static final String URI_DELIMITER = "\t";
	private static final String SERVICE_DELIMITER = ":";

	private final InetSocketAddress addr;
	private final String domainId;
	private final String serviceName;
	private final String serviceURI;
	private MulticastSocket ms;

	private final Map<String, SpreadsheetClient> spreadsheetClients = new HashMap<>();
	private final Map<String, UsersClient> usersClients = new HashMap<>();

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	public Discovery(InetSocketAddress addr, String domainId, String serviceName, String serviceURI) {
		this.addr = addr;
		this.domainId = domainId;
		this.serviceName = serviceName;
		this.serviceURI  = serviceURI;
		this.ms = null;
	}

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	public Discovery(String domainId, String serviceName, String serviceURI) {
		this(DISCOVERY_ADDR, domainId, serviceName, serviceURI);
	}
	
	/**
	 * Starts sending service announcements at regular intervals... 
	 */
	public void startSendingAnnouncements() {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", addr, serviceName, serviceURI));

		byte[] announceBytes = (domainId+ SERVICE_DELIMITER +serviceName+ URI_DELIMITER +serviceURI).getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			if(ms == null) {
				ms = new MulticastSocket(addr.getPort());
				ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			}

			// start thread to send periodic announcements
			new Thread(() -> {
				for (;;) {
					try {
						ms.send(announcePkt);
						Thread.sleep(DISCOVERY_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
						// do nothing
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts collecting service announcements at regular intervals...
	 */
	public void startCollectingAnnouncements() {
		try {
			if(ms == null) {
				ms = new MulticastSocket(addr.getPort());
				ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			}

			// start thread to collect announcements
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);
				for (;;) {
					try {
						pkt.setLength(1024);
						ms.receive(pkt);

						String msg = new String( pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(URI_DELIMITER);

						if( msgElems.length == 2) {	//periodic announcement

							String sn = msgElems[0], su = msgElems[1];

							URI uri = URI.create(su);
							String[] split = sn.split(SERVICE_DELIMITER);

							this.addClient(split[0],split[1],uri.toString());
						}
					} catch (IOException ignored) {
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addClient(String domain, String service, String serverUrl) {
		if(domain.equals(domainId) && service.equals(serviceName))
			return;

		if(service.equals(UsersClient.SERVICE) && !usersClients.containsKey(domain)) {
			try {
				UsersCachedClient client = new UsersCachedClient(serverUrl);
				usersClients.put(domain, client);
			} catch (Exception ignored) {
			}
		}
		else if(service.equals(SpreadsheetClient.SERVICE) && !spreadsheetClients.containsKey(domain)) {
			try {
				SpreadsheetCachedClient client = new SpreadsheetCachedClient(serverUrl);
				spreadsheetClients.put(domain, client);
			} catch (Exception ignored) {
			}
		}
	}

	public UsersClient getUserClient(String domainId) {
		return usersClients.get(domainId);
	}

	public SpreadsheetClient getSpreadsheetClient(String domainId) {
		return spreadsheetClients.get(domainId);
	}
}
