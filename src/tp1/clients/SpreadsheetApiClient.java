package tp1.clients;

import tp1.api.Spreadsheet;

import java.util.Set;

public interface SpreadsheetApiClient {

    String SERVICE = "SpreadsheetsService";

    String createSpreadsheet(Spreadsheet sheet, String password );

    void deleteSpreadsheet(String sheetId, String password);

    Spreadsheet getSpreadsheet(String sheetId , String userId, String password);

    String[][] getSpreadsheetValues(String sheetId, String userId, String password);

    String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range);

    void updateCell( String sheetId, String cell, String rawValue, String userId, String password);

    void shareSpreadsheet(String sheetId, String userId, String password);

    void unshareSpreadsheet( String sheetId, String userId,  String password);

    void deleteUserSpreadsheets(String userId, String password);
}