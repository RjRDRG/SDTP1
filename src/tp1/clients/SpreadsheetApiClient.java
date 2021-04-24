package tp1.clients;

import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;

import java.util.Set;

public interface SpreadsheetApiClient {

    String SERVICE = "sheets";

    String createSpreadsheet(Spreadsheet sheet, String password ) throws SheetsException;

    void deleteSpreadsheet(String sheetId, String password) throws SheetsException;

    Spreadsheet getSpreadsheet(String sheetId , String userId, String password) throws SheetsException;

    String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException;

    String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range) throws SheetsException;

    void updateCell( String sheetId, String cell, String rawValue, String userId, String password) throws SheetsException;

    void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException;

    void unshareSpreadsheet( String sheetId, String userId,  String password) throws SheetsException;

    void deleteUserSpreadsheets(String userId, String password) throws SheetsException;
}