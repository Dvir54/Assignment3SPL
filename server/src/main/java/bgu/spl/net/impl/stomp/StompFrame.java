package bgu.spl.net.impl.stomp;

import java.util.concurrent.ConcurrentHashMap;

public class StompFrame {
    
    private String command;
    private ConcurrentHashMap<String, String> headers;
    private String body;
    private String rawMessage;

    private StompFrame(String command, ConcurrentHashMap<String, String> headers, String body, String rawMessage) {
        this.command = command;
        this.headers = headers;
        this.body = body;
        this.rawMessage = rawMessage;
    }

    public static StompFrame createStompFrame(String rawMessage) {
        String[] lines = rawMessage.split("\n");
        String command = lines[0];
        ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();
        String body = "";
        int i = 1;
        while (i < lines.length && !lines[i].isEmpty()) {
            String[] header = lines[i].split(":", 2);
            headers.put(header[0], header[1]);
            i++;
        }
        body = i + 1 < lines.length ? String.join("\n", lines[i + 1]) : "";
        return new StompFrame(command, headers, body, rawMessage);
    }

    public String getCommand() {
        return command;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public String getBody() {
        return body;
    }

    public String getRawMessage() {
        return rawMessage;
    }
}
