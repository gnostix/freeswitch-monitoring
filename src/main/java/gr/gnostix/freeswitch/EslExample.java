package gr.gnostix.freeswitch;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;

import java.io.Serializable;

public class EslExample {
    public static class Greet implements Serializable {}
    public static class WhoToGreet implements Serializable {
        public final String who;
        public WhoToGreet(String who) {
            this.who = who;
        }
    }
    public static class Greeting implements Serializable {
        public final String message;
        public Greeting(String message) {
            this.message = message;
        }
    }

    public static class Greeter extends UntypedActor {
        String greeting = "";

        public void onReceive(Object message) {
            if (message instanceof WhoToGreet)
                greeting = "hello, " + ((WhoToGreet) message).who;

            else if (message instanceof Greet)
                // Send the current greeting back to the sender
                getSender().tell(new Greeting(greeting), getSelf());

            else unhandled(message);
        }
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("HelloFS");
      MyEslConnection myConn = new MyEslConnection(system);
      system.awaitTermination();
        myConn.deinitConnection();
    }

    public static class GreetPrinter extends UntypedActor {
        public void onReceive(Object message) {
            if (message instanceof Greeting)
                System.out.println(((Greeting) message).message);
        }
    }
}
