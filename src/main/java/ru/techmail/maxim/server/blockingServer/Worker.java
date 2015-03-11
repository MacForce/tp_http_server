package ru.techmail.maxim.server.blockingServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.techmail.maxim.server.exceptions.ForbiddenException;
import ru.techmail.maxim.server.exceptions.NotSupportMethodException;
import ru.techmail.maxim.server.exceptions.RequestFormatException;
import ru.techmail.maxim.server.exceptions.FileNotFoundException;
import ru.techmail.maxim.server.filesContainer.*;
import ru.techmail.maxim.server.http.Method;
import ru.techmail.maxim.server.http.Request;
import ru.techmail.maxim.server.http.Response;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import ru.techmail.maxim.server.filesContainer.File;

public class Worker implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
    private Request request;
    public Response response;
	private Socket socket;
    private AtomicInteger threadsCount;
	
	public Worker(Socket socket, AtomicInteger threadsCount) {
        this.threadsCount = threadsCount;
		this.socket = socket;
        request = new Request();
        response = new Response();
	}

	@Override
	public void run() {
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream writeStream = socket.getOutputStream()) {
            try {
                readRequest(reader);
            } catch (NotSupportMethodException e) {
                response.setStatusCode(400);
                sendResponse(writeStream, "HTTP method not supported");
                return;
            } catch (RequestFormatException e) {
                response.setStatusCode(400);
                sendResponse(writeStream, "Invalid format of HTTP request");
                return;
            }
            if (!checkUri(writeStream)) return;
            try {
                sendResponse(writeStream, FileServer.getFile(request.getUri()));
            } catch(FileNotFoundException e) {
                response.setStatusCode(404);
                sendResponse(writeStream, "File not found");
            } catch(ForbiddenException e) {
                response.setStatusCode(403);
                sendResponse(writeStream, "Forbidden");
            }
        } catch (Throwable e) {
            LOG.error("Exception: ", e);
        }
		LOG.debug("Closing socket, terminating thread");
        threadsCount.decrementAndGet();
	}

    private boolean checkUri(OutputStream writeStream) {
        if(request.getUri().contains("..")) {
            response.setStatusCode(400);
            sendResponse(writeStream, "Nope!");
            return false;
        }
        return true;
    }
	
	private void sendResponse(OutputStream socketWriter, File file) {
		byte[] body = file.getContents();
		int length = file.getLength();
		response.setBody(body);
		response.setHeader("Server", "Best Java web server by Maxim");
		response.setHeader("Date", getServerTime());
		response.setHeader("Connection", "closed");
		response.setHeader("Content-Type", file.getContentType());
		response.setHeader("Content-Length", String.valueOf(length));
		LOG.debug("Content-Length for {}: {}", request.getUri(), length);
		byte[] headers = response.getHeaders().getBytes();
		try {
            // если отправляем ошибку или HEAD
            if (response.getStatusCode() != 200) {
                byte[] fullResponse = new byte[headers.length + body.length];
                System.arraycopy(headers, 0, fullResponse, 0, headers.length);
                System.arraycopy(body, 0, fullResponse, headers.length, body.length);
                socketWriter.write(fullResponse, 0, fullResponse.length);
            } else {
                socketWriter.write(headers, 0, headers.length);
                if (request.getMethod() == Method.GET) {
                    long sendingSize;
                    if ((sendingSize = Files.copy(file.getPath(), socketWriter)) < file.getLength()) {
                        LOG.error("File wasn't sent fully: " + sendingSize + " of " + file.getLength());
                    }
                }
            }
		} catch (IOException e) {
			LOG.error("Can't write response: ", e);
		}
	}
	
	private void sendResponse(OutputStream socketWriter, String msg) {
		try {
			byte body[] = msg.getBytes("UTF-8");
			sendResponse(socketWriter, new File(body, body.length, "Text"));
		} catch (UnsupportedEncodingException e) {
			LOG.error("Wrong Charset of message: ", e);
		}
	}
	
	private void readRequest(BufferedReader socketReader) throws IOException, RequestFormatException, NotSupportMethodException {
		readHeaders(socketReader);
		String contentLength = request.getHeader("Content-Length");
		if(contentLength != null) {
			readBody(socketReader, Integer.parseInt(contentLength));
		}
	}
	
	private void readHeaders(BufferedReader socketReader) throws IOException, RequestFormatException, NotSupportMethodException {
        LOG.debug("Reading request headers");
        String inputLine = socketReader.readLine();
        request.parseInitialLine(inputLine);
        while ((inputLine = socketReader.readLine()) != null && !inputLine.isEmpty()) {
            request.parseHeaderLine(inputLine);
        }
	}
	
	private void readBody(BufferedReader socketReader, int length) throws IOException {
		LOG.debug("Reading request body");
		char[] body = new char[length];
		socketReader.read(body, 0, length);
		request.setBody(String.valueOf(body).getBytes());
	}
	
	private String getServerTime() {
	    Calendar calendar = Calendar.getInstance();
	    SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}
}
