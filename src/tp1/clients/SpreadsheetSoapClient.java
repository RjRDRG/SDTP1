package tp1.clients;

import com.sun.xml.ws.client.BindingProviderProperties;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SoapException;
import tp1.api.service.soap.SoapSpreadsheets;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.function.Supplier;

public class SpreadsheetSoapClient implements SpreadsheetApiClient {

    public final static String SPREADSHEETS_WSDL = "/spreadsheets/?wsdl";

    public final static int MAX_RETRIES = 3;
    public final static long RETRY_PERIOD = 1000;
    public final static int CONNECTION_TIMEOUT = 1000;
    public final static int REPLY_TIMEOUT = 600;

    public final SoapSpreadsheets target;

    public SpreadsheetSoapClient (String serverUrl) throws MalformedURLException {
        QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
        Service service = Service.create( new URL(serverUrl + SPREADSHEETS_WSDL), QNAME );
        target = service.getPort( SoapSpreadsheets.class );

        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        ((BindingProvider) target).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
    }

    private <T> T retry(Supplier<T> supplier) {
        SoapException exception;

        int retries=0;
        do {
            retries++;

            try {
                return supplier.get();
            } catch (SoapException e) {
                exception = new SoapException(e.getMessage());
                break;
            } catch (Exception e) {
                exception = new SoapException(e.getMessage());
            }

            try { Thread.sleep(RETRY_PERIOD); } catch (InterruptedException ignored) {}

        } while (retries < MAX_RETRIES);

        throw exception;
    }

    @Override
    public String createSpreadsheet(Spreadsheet sheet, String password) {
        return retry( () -> target.createSpreadsheet(sheet, password) );
    }

    @Override
    public void deleteSpreadsheet(String sheetId, String password) {
        retry( () -> { target.deleteSpreadsheet(sheetId, password);
            return null;
        });
    }

    @Override
    public Spreadsheet getSpreadsheet(String sheetId, String userId, String password)  {
        return retry( () -> target.getSpreadsheet(sheetId, userId, password) );
    }

    @Override
    public String[][] getSpreadsheetValues(String sheetId, String userId, String password)  {
        return retry( () -> target.getSpreadsheetValues(sheetId, userId, password) );
    }

    @Override
    public String[][] getReferencedSpreadsheetValues(String sheetId, String userId, String range)  {
        return retry( () -> target.getReferencedSpreadsheetValues(sheetId, userId, range) );
    }

    @Override
    public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)  {
        retry( () -> { target.updateCell(sheetId, cell, rawValue, userId, password);
            return null;
        } );
    }

    @Override
    public void shareSpreadsheet(String sheetId, String userId, String password)  {
        retry( () -> { target.shareSpreadsheet(sheetId, userId, password);
            return null;
        } );
    }

    @Override
    public void unshareSpreadsheet(String sheetId, String userId, String password)  {
        retry( () -> { target.unshareSpreadsheet(sheetId, userId, password);
            return null;
        } );
    }

    @Override
    public void deleteUserSpreadsheets(String userId, String password) {
        return retry( () -> target.deleteUserSpreadsheets(userId, password) );
    }
}
