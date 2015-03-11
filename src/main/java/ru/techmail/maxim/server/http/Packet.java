package ru.techmail.maxim.server.http;

import java.util.HashMap;
import java.util.Map;

public class Packet {
	protected Version version = Version.HTTP10;
	protected Map<String, String> headers = new HashMap<String, String>();
	protected byte[] body;
	
	public Version getVersion() {
		return version;
	}
	
	public void setVersion(Version version) {
		this.version = version;
	}
	
	public String getHeader(String name) {
		return headers.get(name);
	}
	
	public void setHeader(String name, String value) {
		headers.put(name, value);
	}
	
	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}
}
