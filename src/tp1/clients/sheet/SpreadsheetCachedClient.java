package tp1.clients.sheet;

import tp1.api.Spreadsheet;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.util.Result;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.util.CellRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpreadsheetCachedClient implements SpreadsheetClient{

    public final static long UPDATE_PERIOD = 500;

    private final SpreadsheetClient client;

    private final Map<String, Spreadsheet> sheetsCache;
    private final SpreadsheetEngine engine;

    public SpreadsheetCachedClient(SpreadsheetClient client) {
        this.client = client;
        this.sheetsCache = new HashMap<>();
        this.engine = SpreadsheetEngineImpl.getInstance();

        startCollecting();
    }

    public SpreadsheetCachedClient(String serverUrl) throws Exception{
        SpreadsheetClient c;
        if (serverUrl.contains("/rest"))
            c = new SpreadsheetRestClient(serverUrl);
        else
            c = new SpreadsheetSoapClient(serverUrl);

        this.client = new SpreadsheetRetryClient(c);
        this.sheetsCache = new HashMap<>();
        this.engine = SpreadsheetEngineImpl.getInstance();

        startCollecting();
    }

    private void startCollecting() {
        new Thread(() -> {
            for (;;) {
                try {
                    List<Spreadsheet> result = client.getSpreadsheets().value();
                    for (Spreadsheet s : result) {
                        sheetsCache.put(s.getSheetId(),s);
                    }
                } catch (Exception ignored) {
                }

                try { Thread.sleep(UPDATE_PERIOD); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    @Override
    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {
        return client.createSpreadsheet(sheet, password);
    }

    @Override
    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        return client.deleteSpreadsheet(sheetId,password);
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        Result<Spreadsheet> result = client.getSpreadsheet(sheetId,userId, password);

        if(result.isOK() || result.error() != Result.ErrorCode.NOT_AVAILABLE)
            return result;
        else {
            Spreadsheet s = sheetsCache.get(sheetId);

            if(s != null)
                return Result.ok(s);
            else
                return result;
        }
    }

    @Override
    public Result<String[][]> getSpreadsheetValues(String sheetId, String userId, String password) {
        Result<String[][]> result = client.getSpreadsheetValues(sheetId,userId, password);

        if(result.isOK() || result.error() != Result.ErrorCode.NOT_AVAILABLE)
            return result;
        else {
            Spreadsheet s = sheetsCache.get(sheetId);

            if(s != null) {
                String[][] values = null;
                try {
                    values = engine.computeSpreadsheetValues(s);
                } catch (Exception ignored) {
                }

                if (values != null)
                    return Result.ok(values);
                else
                    return result;
            }
            else return result;
        }
    }

    @Override
    public Result<String[][]> getReferencedSpreadsheetValues(String sheetId, String userId, String range) {
        Result<String[][]> result = client.getReferencedSpreadsheetValues(sheetId,userId, range);

        if(result.isOK() || result.error() != Result.ErrorCode.NOT_AVAILABLE)
            return result;
        else {
            Spreadsheet s = sheetsCache.get(sheetId);

            if(s != null) {

                String[][] values = null;
                try {
                    values = engine.computeSpreadsheetValues(s);
                } catch (Exception ignored) {
                }

                if (values != null)
                    return Result.ok(new CellRange(range).extractRangeValuesFrom(values));
                else
                    return result;
            }
            else return result;
        }
    }


    @Override
    public Result<Void> updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
        return client.updateCell(sheetId, cell, rawValue, userId, password);
    }

    @Override
    public Result<Void> shareSpreadsheet(String sheetId, String userId, String password) {
        return client.shareSpreadsheet(sheetId,userId,password);
    }

    @Override
    public Result<Void> unshareSpreadsheet(String sheetId, String userId, String password) {
        return client.unshareSpreadsheet(sheetId,userId,password);
    }

    @Override
    public Result<Void> deleteUserSpreadsheets(String userId, String password) {
        return client.deleteUserSpreadsheets(userId,password);
    }

    @Override
    public Result<List<Spreadsheet>> getSpreadsheets() {
        Result<List<Spreadsheet>> result = client.getSpreadsheets();

        if(result.isOK() || result.error() != Result.ErrorCode.NOT_AVAILABLE)
            return result;
        else
            return Result.ok(new ArrayList<Spreadsheet>(sheetsCache.values()));
    }
}
