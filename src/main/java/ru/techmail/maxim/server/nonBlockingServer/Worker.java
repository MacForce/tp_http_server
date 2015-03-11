package ru.techmail.maxim.server.nonBlockingServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.techmail.maxim.server.exceptions.*;
import ru.techmail.maxim.server.filesContainer.CustomFile;
import ru.techmail.maxim.server.filesContainer.FileServer;
import ru.techmail.maxim.server.http.Method;
import ru.techmail.maxim.server.http.Request;
import ru.techmail.maxim.server.http.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class Worker {
	private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
    private ByteBuffer buff;
    private SocketChannel clientSocket;
    private Request request;
    private Response response;
    private CustomFile file;
    private boolean isSendHeaders;
    private boolean isSendFile;

	public Worker(SocketChannel socket) {
        isSendHeaders = true;
        isSendFile = true;
		clientSocket = socket;
        request = new Request();
        response = new Response();
        buff = ByteBuffer.allocateDirect(4 * 1024); // max size: 8 * 1024
	}

    public void read(SelectionKey key) throws IOException {
        if (request.getMethod() == null &&
                (clientSocket.read(buff) == -1 || buff.get(buff.position()-1) == '\n')) {
            readRequest(key);
            buff.clear();
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public void write(SelectionKey key) throws IOException {
        if (isSendHeaders) {
            if (response.getStatusCode() == 200) {
                setResponseHeaders(file.getLength(), file.getContentType());
                // если количество занятых байт в buff + длина файла <= 1024 (max 1452, но лучше с запасом)
                // то передаём файл в buff целиком
                // иначе - в следующих пакетах (transferTo)
                clientSocket.write(buff);
            } else {
                setResponseHeaders();
                clientSocket.write(buff);
            }
            if (buff.remaining() == 0) {
                buff.clear();
                isSendHeaders = false;
            }
        } else {
            if (isSendFile) {
                // transfer file
                try {
                    if (FileServer.sendFile(file, clientSocket)) {
                        isSendFile = false;
                    }
                } catch (FileNotFoundException | ForbiddenException e) {
                    LOG.error("Transfering file is failed!", e);
                    isSendFile = false;
                }
            }
        }
        if (!isSendHeaders && !isSendFile) {
            clientSocket.close();
            key.cancel();
        } else {
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void readRequest(SelectionKey key) {
        buff.flip();
        String reqFull = Charset.forName("8859_1").decode(buff).toString();
        if (reqFull.length() < 5) {
            setErrorResponse(403,"Fack you");
            LOG.error("Empty socket, gotten: " + reqFull);
            key.interestOps(SelectionKey.OP_WRITE);
            return;
        }
        LOG.debug("request:\n" + reqFull);
        try {
            // parse first line
            String initLine = reqFull.substring(0, reqFull.indexOf("\r\n"));
            request.parseInitialLine(initLine);
            if (initLine.length() < reqFull.length()) {
                // parse headers
                if (reqFull.indexOf("\r\n") < reqFull.indexOf("\r\n\r\n")) {
                    String[] headers = reqFull.substring(reqFull.indexOf("\r\n") + 1, reqFull.indexOf("\r\n\r\n")).split("\r\n");
                    for (String header : headers) {
                        request.parseHeaderLine(header);
                    }
                }
            }
            if (checkRequest()) {
                file = FileServer.getCustomFile(request.getUri());
                if (request.getMethod() == Method.HEAD) {
                    file.close();
                    isSendFile = false;
                }
            }
        } catch (RequestFormatException e) {
            setErrorResponse(400, "Invalid format of HTTP request");
        } catch (NotSupportMethodException e) {
            setErrorResponse(400, "HTTP method not supported");
        } catch (FileNotFoundException e) {
            setErrorResponse(404, "File not found");
        } catch (ForbiddenException e) {
            setErrorResponse(403, "Forbidden");
        } catch (Exception e) {
            LOG.error("Exception: ", e);
            setErrorResponse(500, "Some problems on server");
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private boolean checkRequest() {
        if(request.getUri().contains("..")) {
            setErrorResponse(400, "Nope!");
            return false;
        }
        return true;
    }

    private void setResponseHeaders() {
        response.setHeader("Server", "Best Java web server by Maxim");
        response.setHeader("Date", getServerTime());
        response.setHeader("Connection", "closed");
        LOG.debug("Content-Length for {}: {}", request.getUri(), response.getHeader("Content-Length"));
        buff.clear();
        if (Integer.valueOf(response.getHeader("Content-Length")) > 0) {
            Charset.forName("8859_1").newEncoder().encode(
                    CharBuffer.wrap(response.getHeaders() + new String(response.getBody())), buff, true);
        } else {
            Charset.forName("8859_1").newEncoder().encode(
                    CharBuffer.wrap(response.getHeaders()), buff, true);
        }
        buff.flip();
    }

    private void setResponseHeaders(int fileLength, String contentType) {
        response.setHeader("Server", "Best Java web server by Maxim");
        response.setHeader("Date", getServerTime());
        response.setHeader("Connection", "closed");
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Length", String.valueOf(fileLength));
        LOG.debug("Content-Length for {}: {}", request.getUri(), fileLength);
        buff.clear();
        Charset.forName("8859_1").newEncoder().encode(
                CharBuffer.wrap(response.getHeaders()), buff, true);
        buff.flip();
    }

    private void setErrorResponse(int code, String msg) {
        isSendFile = false;
        response.setStatusCode(code);
        try {
            byte body[] = msg.getBytes("UTF-8");
            response.setBody(body);
            response.setHeader("Content-Length", String.valueOf(body.length));
            response.setHeader("Content-Type", "Text");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Wrong Charset of message: ", e);
        }
    }

    private String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}
