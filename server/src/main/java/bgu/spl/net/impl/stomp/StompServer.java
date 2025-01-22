package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        
        if (args.length != 2) {
            System.err.println("Usage: StompServer <port> <server type>");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
            return;
        }

        String serverType = args[1];

        if (serverType.equalsIgnoreCase("tpc")) {
            // Start a Thread-Per-Client server
            Server.threadPerClient(
                port,
                StompMessageProtocolImpl::new,    // Protocol factory
                StompMessageEncoderDecoder::new  // Encoder/decoder factory
            ).serve();
        }
         else if (serverType.equalsIgnoreCase("reactor")) {
            // Start a Reactor server
            Server.reactor(
                Runtime.getRuntime().availableProcessors(),
                port,
                StompMessageProtocolImpl::new,    // Protocol factory
                StompMessageEncoderDecoder::new  // Encoder/decoder factory
            ).serve();
        }
        else {
            System.err.println("Invalid server type: " + serverType);
            System.exit(1);
        }
    }
}
