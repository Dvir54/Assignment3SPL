package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> activeClients;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> chanelSubscription;
    private ConcurrentHashMap<Integer, User> activeUsers;
    private ConcurrentLinkedQueue<User> users;
    private int counterMessageId = 0;

    public ConnectionsImpl(){
        activeClients = new ConcurrentHashMap<>();
        chanelSubscription = new ConcurrentHashMap<>();
        activeUsers = new ConcurrentHashMap<>();
        users = new ConcurrentLinkedQueue<>();
    }

    public boolean send(int connectionId, T msg){
        if(!activeClients.containsKey(connectionId)){
            return false;
        }
        else {
            activeClients.get(connectionId).send(msg);
            return true;
        }
    }
    
    public void send(String channel, T msg){
        for (Integer connectionID : chanelSubscription.get(channel)){
            send(connectionID, msg);
        }
    }

    public void connect(int connectionId, User user){
        if(!users.contains(user)){
            users.add(user);
        }
        else{
            user.setLoggedIn(true);
            user.setConnectionId(connectionId);
        }
        activeUsers.put(connectionId, user);
    }

    public void disconnect(int connectionId){
        disconnectUser(connectionId);
        activeUsers.remove(connectionId);
        ConnectionHandler<T> connectionHandler = activeClients.remove(connectionId);
        if(connectionHandler != null){
            try {
                connectionHandler.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }    
        removeFromChanels(connectionId);
    }

    public ConcurrentHashMap<Integer, ConnectionHandler<T>> getActiveClients(){
        return activeClients;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> getChanelSubscription(){
        return chanelSubscription;
    }

    public void removeFromChanels(int connectionId){
        for (ConcurrentLinkedQueue<Integer> queue : chanelSubscription.values()) {
            queue.remove(connectionId);
        }
    }

    public void subscribeClient(int connectionId,  String chanel){
        if(!chanelSubscription.containsKey(chanel)){
            chanelSubscription.put(chanel, new ConcurrentLinkedQueue<>());
        }
        chanelSubscription.get(chanel).add(connectionId);
    }

    public void unSubscribeClient(int connectionId,  String chanel){
        chanelSubscription.get(chanel).remove(connectionId);
    }

    private void disconnectUser(int connectionId){
        User user = activeUsers.get(connectionId);
        user.setLoggedIn(false);
        user.setConnectionId(-1);
    }

    public User userIsExists(String userName){
        for (User user : users){
            if(user.getUserName().equals(userName)){
                return user;
            }
        }
        return null;
    }

    public int getCounterMessageId(){
        return counterMessageId;
    }

    public void incrementCounterMessageId(){
        counterMessageId++;
    }

    public ConcurrentLinkedQueue<Integer> getSubscribersToChanel(String chanel){
        return chanelSubscription.get(chanel);
    }
}

