package tp1.clients;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

public class SpreadsheetSoapClient implements SpreadsheetApiClient {

    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    public final static int MAX_RETRIES = 5;
    public final static long RETRY_PERIOD = 1000;
    public final static int CONNECTION_TIMEOUT = 10000;
    public final static int REPLY_TIMEOUT = 1000;

    public final SoapSpreadsheets target;

    public SpreadsheetSoapClient (String serverUrl) throws MalformedURLException {
        QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
        Service service = Service.create( new URL(serverUrl + SPREADSHEETS_WSDL), QNAME );
        target = service.getPort( SoapSpreadsheets.class );

        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws SheetsException;
    }

    private <T> T retry(CheckedSupplier<T> supplier) throws SheetsException {
        SheetsException exception;

        int retries=0;
        do {
            retries++;

            try {
                return supplier.get();
            } catch (SheetsException e) {
                exception = e;
                break;
            } catch (Exception e) {
                exception = new SheetsException(e.getMessage());
            }

            try { Thread.sleep(RETRY_PERIOD); } catch (InterruptedException ignored) {}

        } while (retries < MAX_RETRIES);

        throw exception;
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
        return retry( () -> target.createSpreadsheet(sheet, password) );
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
        retry( () -> { target.deleteSpreadsheet(sheetId, password);
            return null;
        });
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password)  throws SheetsException {
        return retry( () -> target.getSpreadsheet(sheetId, userId, password) );
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password)  throws SheetsException{
        return retry( () -> target.getSpreadsheetValues(sheetId, userId, password) );
    }

    @Override
    public String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range)  throws SheetsException{
        return retry( () -> target.getReferencedSpreadsheetValues(sheetId, userId, range) );
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)  throws SheetsException{
        retry( () -> { target.updateCell(sheetId, cell, rawValue, userId, password);
            return null;
        } );
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password)  throws SheetsException{
        retry( () -> { target.shareSpreadsheet(sheetId, userId, password);
            return null;
        } );
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password)  throws SheetsException{
        retry( () -> { target.unshareSpreadsheet(sheetId, userId, password);
            return null;
        } );
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) throws SheetsException {
        retry( () -> {target.deleteUserSpreadsheets(userId, password); return null; } );
    }
}
