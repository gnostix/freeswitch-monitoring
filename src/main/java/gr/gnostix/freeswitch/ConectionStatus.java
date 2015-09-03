package gr.gnostix.freeswitch;

/**
 * Created by rebel on 3/9/15.
 */
public class ConectionStatus {

    public ConectionStatus(boolean connected, String message) {
        this.connected = connected;
        this.message = message;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getMessage() {
        return message;
    }

    private boolean connected;
    private String message;


}
