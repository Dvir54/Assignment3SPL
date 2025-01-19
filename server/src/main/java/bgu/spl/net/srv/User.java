package bgu.spl.net.srv;

public class User {

    private String userName;
    private String passcode;
    private boolean isLoggedIn;
    private int connectionId;

    public User(String userName, String passcode, int connectionId) {
        this.userName = userName;
        this.passcode = passcode;
        this.isLoggedIn = true;
        this.connectionId = connectionId;
    }

    public String getUserName() {
        return userName;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public String getPasscode() {
        return passcode;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    
}
