package ru.techmail.maxim.server.filesContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.techmail.maxim.server.exceptions.FileNotFoundException;
import ru.techmail.maxim.server.exceptions.ForbiddenException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FileServer {
	private static final Logger LOG = LoggerFactory.getLogger(FileServer.class);
	private static String FILES_DIR = "./files";
	private static final String DEFAULT_FILE = "index.html";
	private static final Map<String, String> CONTENT_TYPES;
	
	static {
		Map<String, String> types = new HashMap<>();
		types.put(".html", "text/html");
		types.put(".css", "text/css");
		types.put(".js", "application/x-javascript");
		types.put(".jpg", "image/jpeg");
		types.put(".jpeg", "image/jpeg");
		types.put(".png", "image/png");
		types.put(".gif", "image/gif");
		types.put(".swf", "application/x-shockwave-flash");
		CONTENT_TYPES = Collections.unmodifiableMap(types);
	}

    public static boolean sendFile(CustomFile file, SocketChannel client) throws FileNotFoundException, ForbiddenException, IOException {
        int remaining = file.getLength() - file.getFilePosition();
        long sent = file.getFile().transferTo(file.getFilePosition(), remaining, client);
        if (sent >= remaining || remaining <= 0) {
            file.close();
            return true;
        } else {
            file.setFilePosition(file.getFilePosition() + (int)sent);
            return false;
        }
    }

    private static boolean isFileExists() {
        return true;
    }
	
	public static File getFile(String path) throws FileNotFoundException, ForbiddenException {
		Path completePath;
		if(isDirectory(path)) {
			completePath = Paths.get(FILES_DIR, path, DEFAULT_FILE);
			if(!Files.isRegularFile(completePath)) {
				throw new ForbiddenException();
			}
			LOG.debug("{} is directory", completePath.toString());
		} else {
			completePath = Paths.get(FILES_DIR, path);
			LOG.debug("{} is file", completePath.toString());
		}
        // проверить чтобы completePath был внутри FILES_DIR (DOCUMENT_ROOT)!!!
        if (!completePath.toFile().canRead()) {
            LOG.warn("File not found: {}", completePath);
            throw new FileNotFoundException();
        }
        String extension = getExtension(completePath.toString());
		try {
            return new File((int)Files.size(completePath), CONTENT_TYPES.get(extension), completePath);
		} catch (IOException | SecurityException e) {
			LOG.warn("File not found: {}", completePath, e);
			throw new FileNotFoundException();
		}
	}

    public static CustomFile getCustomFile(String path) throws FileNotFoundException, ForbiddenException {
        Path completePath;
        if (isDirectory(path)) {
            completePath = Paths.get(FILES_DIR, path, DEFAULT_FILE);
            if (!Files.isRegularFile(completePath)) {
                throw new ForbiddenException();
            }
            LOG.debug("{} is directory", completePath.toString());
        } else {
            completePath = Paths.get(FILES_DIR, path);
            LOG.debug("{} is file", completePath.toString());
        }
        // проверить чтобы completePath был внутри FILES_DIR (DOCUMENT_ROOT)!!!
        if (!completePath.toFile().canRead()) {
            LOG.warn("File not found: {}", completePath);
            throw new FileNotFoundException();
        }
        String extension = getExtension(completePath.toString());
        try {
            return new CustomFile((int) Files.size(completePath), CONTENT_TYPES.get(extension),
                    new FileInputStream(completePath.toString()).getChannel());
        } catch (IOException | SecurityException e) {
            LOG.error("Can't get file: {}", completePath, e);
            throw new FileNotFoundException();
        }
    }

	private static boolean isDirectory(String path) {
		return path.charAt(path.length() - 1) == '/';
	}
	
	private static String getExtension(String path) {
		return path.substring(path.lastIndexOf('.'));
	}

    public static void setFilesDir(String dir) {
        FILES_DIR = dir;
    }
}
