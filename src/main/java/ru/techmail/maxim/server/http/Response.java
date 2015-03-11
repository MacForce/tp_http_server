package ru.techmail.maxim.server.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Map.Entry;

public class Response extends Packet {
	static public final Map<Integer, String> STATUS_CODES; //immutable map
	private int statusCode = 200;
	
	static {
		Map<Integer, String> codes = new HashMap<Integer, String>();
		codes.put(200, "OK");
		codes.put(400, "Bad Request");
		codes.put(403, "Forbidden");
		codes.put(404, "Not Found");
		codes.put(301, "Moved Permanently");
		codes.put(302, "Moved Temporarily");
		codes.put(500, "Server Error");
		STATUS_CODES = Collections.unmodifiableMap(codes);
	}
	
	public String getHeaders() {
		StringBuilder response = new StringBuilder();
		String ver = (version == Version.HTTP11 ? "HTTP/1.1" : "HTTP/1.0");
		response.append(String.format("%s %s %s\r\n", ver, statusCode, STATUS_CODES.get(statusCode)));
		for(Entry<String, String> header: headers.entrySet()) {
			response.append(String.format("%s: %s\r\n", header.getKey(), header.getValue()));
		}
		response.append("\r\n");
		return response.toString();
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int code) {
		if(STATUS_CODES.containsKey(code)) {
			this.statusCode = code;
		} else {
			throw new IllegalArgumentException(String.format("Sending code %d is not supported", code));
		}
	}
}
