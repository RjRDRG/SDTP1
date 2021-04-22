package tp1.util;

import jakarta.ws.rs.WebApplicationException;
import tp1.api.service.soap.SoapException;
import tp1.server.WebServiceType;
import jakarta.ws.rs.core.Response.Status;

import java.util.logging.Logger;

import static tp1.server.WebServiceType.SOAP;

public class ExceptionMapper {

    public static void throwWebAppException(Logger Log, String msg, WebServiceType type, Status status) throws RuntimeException {
        Log.info(msg);

        if(type == SOAP)
            throw new SoapException(msg);
        else
            throw new WebApplicationException(status);
    }
}
