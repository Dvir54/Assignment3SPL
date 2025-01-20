package bgu.spl.net.impl.stomp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.srv.User;

public class StompMessageProtocolImpl implements StompMessagingProtocol<String>{

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections;
    private ConcurrentHashMap<String, String> subscriptionIDs;//map of subscription id to each destination

    public StompMessageProtocolImpl() {
        this.subscriptionIDs = new ConcurrentHashMap<>();
    }

    public void start(int connectionId, Connections<String> connections){
        this.connectionId = connectionId;
        this.connections = connections;
    }

    public void process(String message){
        StompFrame stompMessage = StompFrame.createStompFrame(message);
        switch (stompMessage.getCommand()) {
            case "CONNECT":
                handleConnect(stompMessage);
                break;
            case "SEND":
                handleSend(stompMessage);
                break;
            case "SUBSCRIBE":
                handleSubscribe(stompMessage);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(stompMessage);
                break;
            case "DISCONNECT":
                handleDisconnect(stompMessage);
                break;
            default:
                sendError("Unknown command", stompMessage ,null, "The command is not recognized by the server");
                break;
        }
    }

    public boolean shouldTerminate(){
        return shouldTerminate;
    }

    public ConcurrentHashMap<String, String> getSubscriptionIDs() {
        return subscriptionIDs;
    }

    private String getSubscriptionId(String destination, ConcurrentHashMap<String, String> subscriptionIDs){
        for (String id : subscriptionIDs.keySet()) {
            if(subscriptionIDs.get(id).equals(destination)){
                return id;
            }
        }
        return null;
    }

    private void handleConnect(StompFrame message) {
        String acceptVersion = message.getHeader("accept-version");
        String receipt = message.getHeader("receipt-id");
        String host = message.getHeader("host");
        String login = message.getHeader("login");
        String passcode = message.getHeader("passcode");

        //check if the message is valid
        if (acceptVersion == null || !acceptVersion.equals("1.2") || host == null || !host.equals("stomp.cs.bgu.ac.il") || login == null || passcode == null) {
            sendError("malformed frame received", message, receipt, "The message is not valid");
            return;
        }
        //check if the user is already exists
        else{
            User user = ((ConnectionsImpl<String>)connections).userIsExists(login);
            if(user != null){
                //check if the password is not correct
                if(!passcode.equals(user.getPasscode())){
                    sendError("Wrong password", message, receipt, "User " + login + "'s password is different than what you inserted");
                }
                //check if the user is already logged in
                else if(user.isLoggedIn()){
                    sendError("User is already logged in", message, receipt, "User " + login + " is already logged in from another client");
                }
                //connect the user
                else{
                    ((ConnectionsImpl<String>)connections).connect(connectionId, user);
                    ((ConnectionsImpl<String>)connections).send(connectionId, "CONNECTED\nversion:1.2\n\n\u0000");
                    if(receipt != null){
                        ((ConnectionsImpl<String>)connections).send(connectionId, "RECEIPT\nreceipt-id:" + receipt + "\n\n\u0000");
                    }
                }
                
            }
            //create a new user
            else{
                user = new User(login, passcode, connectionId);
                ((ConnectionsImpl<String>)connections).connect(connectionId, user);
                ((ConnectionsImpl<String>)connections).send(connectionId, "CONNECTED\nversion:1.2\n\n\u0000");
                if(receipt != null){
                    ((ConnectionsImpl<String>)connections).send(connectionId, "RECEIPT\nreceipt-id:" + receipt + "\n\n\u0000");
                }
            }
        }
    }

    private void handleSend(StompFrame message) {
        String destination = message.getHeader("destination");
        if (destination.startsWith("/")) {
            destination = destination.substring(1); // Remove the leading slash
        }
        String receipt = message.getHeader("receipt");
        String body = message.getBody();

        if (destination == null || body == null) {
            sendError("malformed frame received", message, receipt, "The message is not valid");
        }
        else if (!subscriptionIDs.containsValue(destination)){
            sendError("User is not subscribed to the destination", message, receipt, "User try to send a message to a destination that he is not subscribed to");
        }
        else{
            ((ConnectionsImpl<String>)connections).incrementCounterMessageId();
            ConcurrentLinkedQueue<Integer> subscribers = ((ConnectionsImpl<String>)connections).getSubscribersToChanel(destination);
            for (Integer subscriber : subscribers) {
                String subscriptionId = ((ConnectionsImpl<String>)connections).getActiveClients().get(subscriber).getProtocol().getSubscriptionId(destination, subscriptionIDs);
                connections.send(subscriber, "MESSAGE\nsubscription:" + subscriptionId + "\nmessage-id:" + ((ConnectionsImpl<String>)connections).getCounterMessageId() + "\ndestination:/" + destination + "\n\n" + body + "\u0000");
            }
            if(receipt != null){
                connections.send(connectionId, "RECEIPT\nreceipt-id:" + receipt + "\n\n\u0000");
            }
        }
    }

    private void handleSubscribe(StompFrame message) {
        String destination = message.getHeader("destination");
        String idSubscription = message.getHeader("id");
        String receipt = message.getHeader("receipt");

        //check if the message is valid
        if (destination == null || idSubscription == null || receipt == null) {
            sendError("malformed frame received", message, receipt, "The message is not valid");
        }
        //check if the user is already subscribed to the destination
        else if(subscriptionIDs.containsValue(destination)){
            sendError("User is already subscribed to the destination", message, receipt, "The user can't subscribe to the same destination that he is already subscribed to");
        }
        //check if the subscription id is already in use
        else if(subscriptionIDs.containsKey(idSubscription)){
            sendError("Subscription ID is already in use", message, receipt, "the subscription id is already in use for another destination");
        }
        //subscribe the user
        else{
            subscriptionIDs.put(idSubscription, destination);
            ((ConnectionsImpl<String>)connections).subscribeClient(connectionId, destination);
            connections.send(connectionId, "RECEIPT\nreceipt-id:" + receipt + "\n\n\u0000");
        }     
    }

    private void handleUnsubscribe(StompFrame message) {
        String idSubscription = message.getHeader("id");
        String receipt = message.getHeader("receipt");

        //check if the message is valid
        if (idSubscription == null || receipt == null) {
            sendError("malformed frame received", message, receipt, "The message is not valid");
        }
        //check if the subscription id is not in use
        else if (!subscriptionIDs.containsKey(idSubscription)) {
            sendError("Subscription ID is not in use", message, receipt, "The subscription id is not in use for any destination");
        }
        //unsubscribe the user
        else{
            String destination = subscriptionIDs.remove(idSubscription);
            ((ConnectionsImpl<String>)connections).unSubscribeClient(connectionId, destination);
            connections.send(connectionId, "RECEIPT\nreceipt-id:" + receipt + "\n\n\u0000");
        }
    }

    private void handleDisconnect(StompFrame message) {
        String receipt = message.getHeader("receipt");
        if (receipt != null) {
            String response = "RECEIPT\nreceipt-id:" + receipt + "\n\n\u0000";
            connections.send(connectionId, response);
        }

        shouldTerminate = true;
        connections.disconnect(connectionId, false);
    }

    private void sendError(String errorMessage, StompFrame message, String receipt, String description) {
        if(receipt != null){
            String response = "ERROR\nreceipt-id:" + receipt + "\nmessage:" + errorMessage;

            if (message != null) {
                response += "\nThe message:\n-----\n" + message.getRawMessage() + "\n-----\n" + description;
            }

            response += "\n\u0000";
            connections.send(connectionId, response);
        }
        else{
            String response = "ERROR\nmessage:" + errorMessage;

            if (message != null) {
                response += "\nThe message:\n-----\n" + message.getRawMessage() + "\n-----\n" + description;
            }

            response += "\n\u0000";
            connections.send(connectionId, response);
        }
        connections.disconnect(connectionId, true);
    }

}

