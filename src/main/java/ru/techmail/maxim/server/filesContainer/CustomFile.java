package ru.techmail.maxim.server.filesContainer;

import java.io.IOException;
import java.nio.channels.FileChannel;

public class CustomFile extends File {
    private FileChannel file;
    private int filePosition;

    public CustomFile(int length, String contentType, FileChannel file) {
        super(length, contentType);
        this.file = file;
    }

    public void close() throws IOException {
        file.close();
        file = null;
    }

    public FileChannel getFile() {
        return file;
    }

    public void setFile(FileChannel file) {
        this.file = file;
    }

    public int getFilePosition() {
        return filePosition;
    }

    public void setFilePosition(int filePosition) {
        this.filePosition = filePosition;
    }
}
