package gr.gnostix.freeswitch;

import akka.actor.ActorRef;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.InboundConnectionFailure;

/**
 * Created by rebel on 17/7/15.
 */
public class MyEslConnection {

    private Client conn = null;

    public MyEslConnection(ActorRef callRouter) {
        conn = new Client();
        try {
//            conn.connect("localhost", 8021, "ClueCon", 60);
            conn.connect("192.168.1.128", 8021, "ClueCon", 60);
//            conn.connect("192.168.2.18", 8021, "ClueCon", 60);

            if (conn.canSend() == true) System.out.println("connected");
            //conn.setEventSubscriptions( "plain", "CHANNEL_HANGUP_COMPLETE CHANNEL_CALLSTATE  CHANNEL_CREATE CHANNEL_EXECUTE CHANNEL_EXECUTE_COMPLETE CHANNEL_DESTROY" );
            conn.setEventSubscriptions( "plain", "all" );
            conn.addEventListener(new MyEslEventListener(callRouter));

        } catch (InboundConnectionFailure e) {
            e.printStackTrace();
        }
    }

    public void deinitConnection() {
        conn.close();
        conn = null;
    }
}
