package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.stomp.StompMessageEncoderDecoder;
import bgu.spl.net.impl.stomp.StompMessageProtocolImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer implements Server<String> {

    private final int port;
    private final Supplier<StompMessageProtocolImpl> protocolFactory;
    private final Supplier<StompMessageEncoderDecoder> encdecFactory;
    private ServerSocket sock;
    private ConnectionsImpl<String> connectionsImpl;
    private Integer countUniqeID = 0;

    public BaseServer(int port, Supplier<StompMessageProtocolImpl> protocolFactory, Supplier<StompMessageEncoderDecoder> encdecFactory) {
        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connectionsImpl = new ConnectionsImpl<String>();
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                increaseCounter();
                BlockingConnectionHandler handler = new BlockingConnectionHandler(clientSock, encdecFactory.get(), protocolFactory.get(), this, countUniqeID);
                //ADD the client to the hashMap of activeClients
                connectionsImpl.getActiveClients().put(countUniqeID, handler);
                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    public ConnectionsImpl<String> getConnectionsImpl(){
        return connectionsImpl;
    }

    public void increaseCounter(){
        countUniqeID++;
    }

    public void decreaseCounter(){
        if(countUniqeID > 0){
            countUniqeID--;
        }
    }

    protected abstract void execute(BlockingConnectionHandler handler);

}
