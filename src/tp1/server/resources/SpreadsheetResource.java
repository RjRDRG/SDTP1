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
import java.util.*;
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
	private final Map<String, Set<String>> spreadsheetOwners;

	private final SpreadsheetEngine engine;

	private final WebServiceType type;

	public static Discovery discovery;

	private static Logger Log = Logger.getLogger(SpreadsheetResource.class.getName());

	public SpreadsheetResource(String domainId, WebServiceType type) {
		this.domainId = domainId;
		this.type = type;
		this.spreadsheets = new HashMap<>();
		this.spreadsheetOwners = new HashMap<>();
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

		if(cachedUserClient == null) {
			String serverUrl = discovery.knownUrisOf(domainId, UsersApiClient.SERVICE).stream()
				.findAny()
				.map(URI::toString)
				.orElse(null);

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

		return cachedUserClient;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		if( sheet == null || password == null) {
			throwWebAppException(Log, "Sheet or password null.", type, Response.Status.BAD_REQUEST);
		}

		if (sheet.getColumns() <= 0 || sheet.getRows() <= 0)
			throwWebAppException(Log, "Invalid dimensions.", type, Response.Status.BAD_REQUEST);

		String spreadsheetOwner = sheet.getOwner();

		synchronized(this) {


			//TODO: Evitar ciclos de referencias

			boolean valid = true;
			try {
				valid = getLocalUsersClient().verifyUser(spreadsheetOwner, password);
			} catch (Exception e) {
				throwWebAppException(Log, e.getMessage(), type, Response.Status.BAD_REQUEST);
			}

			if(!valid) throwWebAppException(Log, "Invalid password.", type, Response.Status.BAD_REQUEST);

			String sheetId;
			do {
				sheetId = UUID.randomUUID().toString();
			} while (spreadsheets.containsKey(sheetId));

			Spreadsheet spreadsheet = new Spreadsheet(sheet,sheetId,domainId);

			spreadsheets.put(sheetId, spreadsheet);

			if (!spreadsheetOwners.containsKey(spreadsheetOwner))
				spreadsheetOwners.put(spreadsheetOwner, new TreeSet<>());

			spreadsheetOwners.get(spreadsheetOwner).add(sheetId);

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

			boolean valid = true;
			try {
				valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
			} catch (Exception e) {
				throwWebAppException(Log, e.getMessage(), type, Response.Status.BAD_REQUEST);
			}

			if(!valid) throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);

			spreadsheetOwners.get(sheet.getOwner()).remove(sheetId);
			spreadsheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		if( sheetId == null || userId == null ) {
			throwWebAppException(Log, "SheetId or userId null.", type, Response.Status.BAD_REQUEST);
		}

		Spreadsheet sheet = spreadsheets.get(sheetId);

		if( sheet == null ) {
			throwWebAppException(Log, "Sheet doesnt exist: " + sheetId, type, Response.Status.NOT_FOUND);
		}

		boolean valid = true;
		try {
			valid = getLocalUsersClient().verifyUser(userId, password);
		} catch (Exception e) {
			throwWebAppException(Log, e.getMessage(), type, Response.Status.NOT_FOUND);
		}

		if(!valid) throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);

		if (!userId.equals(sheet.getOwner())) {

			if (!sheet.getSharedWith().stream().anyMatch(user -> user.contains(userId)))
				throwWebAppException(Log, "User " + userId + " does not have permissions to read this spreadsheet.", type, Response.Status.FORBIDDEN);

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

		if (!userId.equals(spreadsheet.getOwner()) && !spreadsheet.getSharedWith().contains(userId)) {
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

			boolean valid = true;
			try {
				valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
			} catch (Exception e) {
				throwWebAppException(Log, e.getMessage(), type, Response.Status.BAD_REQUEST);
			}

			if(!valid) throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);


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

			boolean valid = true;
			try {
				valid = getLocalUsersClient().verifyUser(sheet.getOwner(), password);
			} catch (Exception e) {
				throwWebAppException(Log, e.getMessage(), type, Response.Status.BAD_REQUEST);
			}

			if(!valid) throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);

			Set<String> sharedWith = sheet.getSharedWith();


			if (!sharedWith.contains(userId))
				throwWebAppException(Log, "User " + userId + " is not sharing this spreadsheet therefore it cannot be unshared.",
						type, Response.Status.NOT_FOUND);

			sharedWith.remove(userId);
		}
	}

	@Override
	public void deleteUserSpreadsheets(String userId, String password) {

		synchronized (this) {

			boolean valid = true;

			try {
				valid = getLocalUsersClient().verifyUser(userId, password);
			} catch (Exception e) {
				throwWebAppException(Log, e.getMessage(), type, Response.Status.BAD_REQUEST);
			}

			if(!valid) throwWebAppException(Log, "Invalid password.", type, Response.Status.FORBIDDEN);

			Set<String> sheets = spreadsheetOwners.get(userId);

			sheets.forEach(id -> spreadsheets.remove(id));
			spreadsheetOwners.remove(userId);
		}
	}
}

