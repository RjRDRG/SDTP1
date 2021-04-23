package tp1.server.resources;

import jakarta.inject.Singleton;
import jakarta.jws.WebService;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.clients.*;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.WebServiceType;
import tp1.util.Cell;
import tp1.util.CellRange;
import tp1.util.InvalidCellIdException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static tp1.util.ExceptionMapper.throwWebAppException;

@WebService(
		serviceName = SoapSpreadsheets.NAME,
		targetNamespace = SoapSpreadsheets.NAMESPACE,
		endpointInterface = SoapSpreadsheets.INTERFACE
)
@Singleton
public class SpreadsheetResource implements RestSpreadsheets, SoapSpreadsheets {

	private final String domainId;

	private final Map<String, Spreadsheet> spreadsheets;

	private final SpreadsheetEngine engine;

	private final WebServiceType type;

	public static Discovery discovery;

	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	public SpreadsheetResource(String domainId, WebServiceType type) {
		this.domainId = domainId;
		this.type = type;
		this.spreadsheets = new HashMap<>();
		this.engine = SpreadsheetEngineImpl.getInstance();
	}

	public static void setDiscovery(Discovery discovery) {
		SpreadsheetResource.discovery = discovery;
	}

	private static Map<String, SpreadsheetApiClient> cachedSpreadSheetClients;
	public static SpreadsheetApiClient getRemoteSpreadsheetClient(String domainId) {
		if (cachedSpreadSheetClients == null)
			cachedSpreadSheetClients = new ConcurrentHashMap<>();

		if(cachedSpreadSheetClients.containsKey(domainId))
			return cachedSpreadSheetClients.get(domainId);

		String serverUrl = discovery.knownUrisOf(domainId, SpreadsheetApiClient.SERVICE).stream()
				.findAny()
				.map(URI::toString)
				.orElse(null);

		SpreadsheetApiClient client = null;
		if(serverUrl != null) {
			try {
				if (serverUrl.contains("/rest"))
					client = new SpreadsheetRestClient(serverUrl);
				else
					client = new SpreadsheetSoapClient(serverUrl);

				cachedSpreadSheetClients.put(domainId,client);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return client;
	}


	private UsersApiClient cachedUserClient;
	private UsersApiClient getLocalUsersClient() {
		String sURL = null;

		if(cachedUserClient == null) {
			String serverUrl = discovery.knownUrisOf(domainId, UsersApiClient.SERVICE).stream()
				.findAny()
				.map(URI::toString)
				.orElse(null);

			sURL = serverUrl;
			if(serverUrl != null) {
				try {

					if (serverUrl.contains("/rest"))
						cachedUserClient = new UsersRestClient(serverUrl);
					else
						cachedUserClient = new UsersSoapClient(serverUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (true) throw new RuntimeException(sURL + "fuck you shrek--------------------------------------------------");
		return cachedUserClient;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		if( sheet == null || password == null) {
			throwWebAppException(Log, "Sheet or password null.", type, Response.Status.BAD_REQUEST);
		}

		synchronized(this) {

			//TODO: Evitar ciclos de referencias

			try {
				boolean valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);

				if(!valid)
					throwWebAppException(Log, "Invalid password.", type, Response.Status.BAD_REQUEST);
			} catch (Exception e) {
				throwWebAppException(Log, e.getMessage(), type, Response.Status.BAD_REQUEST);
			}

			String sheetId;
			do {
				sheetId = UUID.randomUUID().toString();
			} while (spreadsheets.containsKey(sheetId));

			Spreadsheet spreadsheet = new Spreadsheet(sheet,sheetId,domainId);

			spreadsheets.put(sheetId, spreadsheet);

			return sheetId;
		}
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {

		if( sheetId == null || password == null ) {
			throwWebAppException(Log, "SheetId or password null.", type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(Log, "Sheet doesnt exist.", type, Response.Status.NOT_FOUND);
			}

			try {
				boolean valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
				if(!valid)
					throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);
			} catch (Exception e) {
				throwWebAppException(Log, "User not found.", type, Response.Status.BAD_REQUEST);
			}

			spreadsheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null || password == null ) {
			throwWebAppException(Log, "SheetId or userId or password null.", type, Response.Status.BAD_REQUEST);
		}

		Spreadsheet sheet = spreadsheets.get(sheetId);

		if( sheet == null ) {
			throwWebAppException(Log, "Sheet doesnt exist.", type, Response.Status.NOT_FOUND);
		}

		if (!sheet.getSharedWith().contains(userId)) {
			throwWebAppException(Log, "User " + userId + " does not have permissions to read this spreadsheet.", type, Response.Status.BAD_REQUEST);
		}

		if (!User.extractDomain(userId).equals(sheet.extractOwnerDomain())) {
			throwWebAppException(Log, "User " + userId + " does not have permissions to read this spreadsheet.", type, Response.Status.BAD_REQUEST);
		}

		try {
			boolean valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
			if(!valid)
				throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);
		} catch (Exception e) {
			throwWebAppException(Log, "User not found.", type, Response.Status.NOT_FOUND);
		}

		return sheet;
	}

	@Override
	public String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
		if( sheetId == null || userId == null || range == null) {
			throwWebAppException(Log, "SheetId or userId or range null.", type, Response.Status.BAD_REQUEST);
		}

		Spreadsheet spreadsheet = spreadsheets.get(sheetId);

		if( spreadsheet == null ) {
			throwWebAppException(Log, "Sheet doesnt exist.", type, Response.Status.NOT_FOUND);
		}

		if (!spreadsheet.getSharedWith().contains(userId)) {
			throwWebAppException(Log, "User " + userId + " does not have permissions to read this spreadsheet.", type, Response.Status.BAD_REQUEST);
		}

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throwWebAppException(Log, "Error in spreadsheet", type, Response.Status.BAD_REQUEST);
		}

		return new CellRange(range).extractRangeValuesFrom(result);
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {

		Spreadsheet spreadsheet = getSpreadsheet(sheetId, userId, password);

		String[][] result = null;
		try {
			result = engine.computeSpreadsheetValues(spreadsheet);
		} catch (Exception exception) {
			throwWebAppException(Log, "Error in spreadsheet", type, Response.Status.BAD_REQUEST);
		}

		return result;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {

		if( sheetId == null || cell == null || rawValue == null || userId == null || password == null) {
			throwWebAppException(Log, "Malformed request.", type, Response.Status.BAD_REQUEST);
		}

		synchronized(this) {

			Spreadsheet spreadsheet = getSpreadsheet(sheetId, userId, password);

			//TODO: Evitar ciclos de referencias

			try {
				Pair<Integer,Integer> coordinates =  Cell.CellId2Indexes(cell);

				spreadsheet.placeCellRawValue(coordinates.getLeft(),coordinates.getRight(), rawValue);
			} catch (InvalidCellIdException e) {
				throwWebAppException(Log, "Invalid spreadsheet cell.", type, Response.Status.BAD_REQUEST);
			}

		}

	}


	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null || password == null ) {
			throwWebAppException(Log, "SheetId or userId or password null.", type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			if( sheet == null ) {
				throwWebAppException(Log, "Sheet doesnt exist.", type, Response.Status.NOT_FOUND);
			}

			try {
				boolean valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
				if(!valid)
					throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);
			} catch (Exception e) {
				throwWebAppException(Log, "User not found.", type, Response.Status.NOT_FOUND);
			}


			Set<String> sharedWith = sheet.getSharedWith();

			if (sharedWith.contains(userId))
				throwWebAppException(Log, "Spreadsheet is already being shared with this user.", type, Response.Status.CONFLICT);

			sharedWith.add(userId);

		}
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null || password == null ) {
			throwWebAppException(Log, "SheetId or userId or password null.", type, Response.Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = spreadsheets.get(sheetId);

			try {
				boolean valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
				if(!valid)
					throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);
			} catch (Exception e) {
				throwWebAppException(Log, "User not found.", type, Response.Status.NOT_FOUND);
			}

			Set<String> sharedWith = sheet.getSharedWith();


			if (!sharedWith.contains(userId))
				throwWebAppException(Log, "User " + userId + " is not sharing this spreadsheet therefore it cannot be unshared.",
						type, Response.Status.NOT_FOUND);

			sharedWith.remove(userId);
		}
	}
}

