package gr.gnostix.freeswitch;

import akka.actor.ActorRef;
import gr.gnostix.freeswitch.actors.ActorsProtocol;
import gr.gnostix.freeswitch.actors.EslEventRouter;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import gr.gnostix.freeswitch.actors.CallRouter;

public class MyEslEventListener implements IEslEventListener {

    public  MyEslEventListener(ActorRef callRouter){
        this.callRouter = callRouter;
    }
    private ActorRef callRouter = null;
    // Create the 'helloakka' actor system
    //final ActorSystem system = ActorSystem.create("HelloFS");

    // Create the event receiver actor
    //final ActorRef eventAct = system.actorOf(Props.create(EslEventRouter.class), "eventAct");


    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public void eventReceived(EslEvent event) {
 //       System.out.println(" --- event received ---" + event.getEventName());
//        System.out.println(getEventToLog(event));
//        System.out.println(" ---------------------------- event received END ------------------------------");

        // eventAct should be the router where the events will be dispatched!!!!!!
        callRouter.tell(ActorsProtocol.mkEvent(event), ActorRef.noSender());

        //System.out.println(getEventBodyLinesToLog(event));
        //log.info("eventReceived [{}]\n[{}]\n", event, getEventToLog(event));
    }

    public void backgroundJobResultReceived(EslEvent event) {
        System.out.println(" --- event backgroundJobResultReceived ---" + event.getEventName());
        log.info("backgroundJobResultReceived [{}]\n[{}]\n", event,
                getEventToLog(event));
    }

    private String getEventToLog(EslEvent event) {
        StringBuffer buf = new StringBuffer();
        Map<String, String> map = event.getEventHeaders();
        Set<String> set = map.keySet();
        for (String name : set) {
            buf.append(name + " " + map.get(name) + "\n");
        }
        return buf.toString();
    }

    public String getEventBodyLinesToLog(EslEvent event) {
        StringBuffer buf = new StringBuffer();
        List<String> li = event.getEventBodyLines();
        for (String name : li) {
            buf.append(name + "\n");
        }
        return buf.toString();
    }
}
