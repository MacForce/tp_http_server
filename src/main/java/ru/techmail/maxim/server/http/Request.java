package ru.techmail.maxim.server.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.techmail.maxim.server.exceptions.NotSupportMethodException;
import ru.techmail.maxim.server.exceptions.RequestFormatException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.PatternSyntaxException;

public class Request extends Packet {
    private static final Logger LOG = LoggerFactory.getLogger(Request.class);
	private Method method;
	private String uri;
	
	@Override
	public String toString() {
		return String.format("%s %s %s", method, uri, version);
	}
	
	public void parseInitialLine(String line) throws RequestFormatException, NotSupportMethodException {
        try {
            method = Enum.valueOf(Method.class, line.substring(0, line.indexOf(' ')).toUpperCase());
        } catch(Exception e) {
            throw new RequestFormatException();
        }
        if (method != Method.GET && method != Method.HEAD) {
            throw new NotSupportMethodException();
        }
        try {
            uri = line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' '));
            if(uri.contains("?")) {
                uri = uri.split("\\?")[0];
            }
            switch(line.substring(line.lastIndexOf(' ') + 1).toUpperCase()) {
                case "HTTP/1.0":
                    version = Version.HTTP10;
                    break;
                case "HTTP/1.1":
                    version = Version.HTTP11;
                    break;
                default:
                    throw new RequestFormatException();
            }
        } catch(IndexOutOfBoundsException | PatternSyntaxException e) {
            throw new RequestFormatException();
        }
	}
	
	public void parseHeaderLine(String line) {
		String[] params = line.split(": ");
		headers.put(params[0], params[1]);
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public String getUri() {
		try {
			return uri != null ? URLDecoder.decode(uri, "UTF-8") : null;
		} catch (UnsupportedEncodingException e) {
			LOG.error("Wrong Charset of uri: ", e);
			return null;
		}
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}
