package gr.gnostix.freeswitch;

import akka.actor.ActorRef;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.InboundConnectionFailure;

/**
 * Created by rebel on 27/8/15.
 */
public class EslConnection {

    private Client conn = null;
    private ActorRef eslMessageRouter;
    private String ip;
    private int port;
    private String password;

    public EslConnection(ActorRef eslMessageRouter, String ip, int port, String password) {
        this.conn = new Client();
        this.eslMessageRouter = eslMessageRouter;
        this.ip = ip;
        this.port = port;
        this.password = password;
    }

    public ActorRef getActor(){
        return this.eslMessageRouter;
    }

    public ConectionStatus connectEsl() {
        try {
            conn.connect(ip, 8021, password, 10);

            if (conn.canSend() == true)
            {
                System.out.println("conn.canSend() connected");
                //conn.setEventSubscriptions("plain", "all");
                conn.setEventSubscriptions( "plain", "CHANNEL_HANGUP_COMPLETE CHANNEL_ANSWER  HEARTBEAT" );
                conn.addEventListener(new MyEslEventListener(eslMessageRouter));
            }
            return new ConectionStatus(true, "all good");
            //conn.setEventSubscriptions( "plain", "CHANNEL_HANGUP_COMPLETE CHANNEL_CALLSTATE  CHANNEL_CREATE CHANNEL_EXECUTE CHANNEL_EXECUTE_COMPLETE CHANNEL_DESTROY" );
        } catch (InboundConnectionFailure e) {
            System.out.println("------- ESL connection failed. !! " + e.getMessage());
            //e.printStackTrace();
            return new ConectionStatus(false, e.getMessage());
        }
    }

    public ConectionStatus checkConnection(){
        ConectionStatus status;
        if (conn.canSend() == true) {
            //System.out.println("connected");
            status = new ConectionStatus(true, "all good");
        } else {
            status = connectEsl();
        }
        return status;
    }

    public void deinitConnection() {
        conn.close();
        conn = null;
    }

    public String getIP(){
        return this.ip;
    }

    public int getPort(){
        return this.port;
    }
}
