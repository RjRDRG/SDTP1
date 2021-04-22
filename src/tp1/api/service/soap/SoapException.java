package tp1.api.service.soap;

import jakarta.xml.ws.WebFault;

@WebFault
public class SoapException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SoapException() {
		super("");
	}

	public SoapException(String errorMessage ) {
		super(errorMessage);
	}

}