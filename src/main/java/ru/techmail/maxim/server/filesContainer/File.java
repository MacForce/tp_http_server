package ru.techmail.maxim.server.filesContainer;

import java.nio.file.Path;

public class  File {
	private byte[] contents;
	private int length;
	private String contentType;
    private Path path;

    // for sending file
    public File(int length, String contentType, Path path) {
        this.length = length;
        this.contentType = contentType;
        this.path = path;
        contents = new byte[0];
    }

    // for error message
    public File(byte[] contents, int length, String contentType) {
        this.contents = (contents != null) ? contents : new byte[0];
        this.length = length;
        this.contentType = contentType;
    }

    public File(int length, String contentType) {
        this.length = length;
        this.contentType = contentType;
        contents = new byte[0];
    }
	
	public byte[] getContents() {
		return contents;
	}
	
	public int getLength() {
		return length;
	}
	
	public String getContentType() {
		return contentType;
	}

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
