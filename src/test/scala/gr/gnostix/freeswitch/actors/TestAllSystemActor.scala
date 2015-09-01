package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.{ActorLogging, Actor, Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, DefaultTimeout, TestKit}
import akka.util.Timeout
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatra.atmosphere.{AtmosphereClient, OutboundMessage}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 29/8/15.
 */
class TestAllSystemActor extends TestKit(ActorSystem("esl-sys"))
with DefaultTimeout with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  //val centralMessageRouterRef = system.actorOf(Props[CentralMessageRouter], "centralMessageRouter")
  //creating a test ws actor
  // val wSLiveEventsActorTest = system.actorOf(Props[WSLiveEventsActorTest], "wSLiveEventsActorTest")

  val centralMessageRouterRef = TestActorRef[CentralMessageRouter]

  val basicStatsActor = centralMessageRouterRef.underlyingActor.basicStatsActor
  val completedCallsActor = centralMessageRouterRef.underlyingActor.completedCallsActor
  val heartBeatActor = centralMessageRouterRef.underlyingActor.heartBeatActor
  val callRouterActor = centralMessageRouterRef.underlyingActor.callRouterActor


  "this test should" should {
    " return an empty list of Completed Calls " in {
      within(100 millis) {
        completedCallsActor ! GetCompletedCalls
        expectMsg(GetCallsResponse(0, List()))
      }
    }
  }


  "this test should" should {
    " return an empty list of Failed Calls " in {
      within(100 millis) {
        callRouterActor ! GetTotalFailedCalls
        expectMsg(TotalFailedCalls(0))
      }
    }
  }


  "this test should" should {
    " return an empty list of Concurrent Calls " in {
      within(100 millis) {
        basicStatsActor ! GetConcurrentCallsTimeSeries
        expectMsg(List())
      }
    }
  }

  "this test should" should {
    " return an empty list of FailedCalls " in {
      within(100 millis) {
        basicStatsActor ! GetFailedCallsTimeSeries
        expectMsg(List())
      }
    }
  }

  "this test should" should {
    " return an empty list of BasicACD " in {
      within(100 millis) {
        basicStatsActor ! GetBasicAcdTimeSeries
        expectMsg(List())
      }
    }
  }



  "this test should" should {
    " return a  list of size 1 with Concurrent Calls " in {
      within(30000 millis) {
        Thread.sleep(12000)
        basicStatsActor ! GetConcurrentCallsTimeSeries
        expectMsgPF() {
          case x@List(ConcurrentCallsTimeSeries(_, _)) => if (x.size == 2) true else false
          case _ => false
        }
        //expectMsg(List(ConcurrentCallsTimeSeries(_, _)))
      }
    }
  }

  "this test should" should {
    " return a list of  size 1 with FailedCalls " in {
      within(30000 millis) {
        Thread.sleep(12000)
        var calls = 0
        basicStatsActor ! GetFailedCallsTimeSeries
        expectMsgPF() {
          case x@List(FailedCallsTimeSeries(_, _)) => calls = x.size
          case _ => calls = 0
        }
        calls should be(1)
      }
    }
  }


  val newChannelDiffCallIdA1 = CallNew("the-uuid-channel-a-1", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "the-uuid-channel-a-1", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10")

  val newChannelDiffCallIdA2 = CallNew("the-uuid-channel-a-2", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "the-uuid-channel-a-1", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10")


  val endChannelDiffCallIdA1 = CallEnd("the-uuid-channel-a-1", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "the-uuid-channel-a-1", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 180, 100, "the-uuid-channel-a-2")

  val endChannelDiffCallIdA2 = CallEnd("the-uuid-channel-a-2", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "call-uuid-other", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 180, 100, "the-uuid-channel-a-1")

  "this test should" should {
    " create a new concurrent call and after call completion should ask for completed calls " in {
      within(60000 millis) {

        var totalCalls = 0

        callRouterActor ! newChannelDiffCallIdA1
        expectNoMsg(FiniteDuration(1, "seconds"))
        callRouterActor ! newChannelDiffCallIdA2
        expectNoMsg(FiniteDuration(1, "seconds"))
        //Thread.sleep(1000)
        callRouterActor ! endChannelDiffCallIdA1
        expectNoMsg(FiniteDuration(1, "seconds"))
        callRouterActor ! endChannelDiffCallIdA2
        expectNoMsg(FiniteDuration(1, "seconds"))

        //Thread.sleep(1000)

        completedCallsActor ! GetCompletedCalls
        expectMsgPF(FiniteDuration(1, "seconds")) {
          case x@GetCallsResponse(_, _) => totalCalls = x.totalCalls
          case _ => totalCalls = -1
        }

        totalCalls should be(1)
      }
    }
  }

  val newChannelSameCallIDA1 = CallNew("the-uuid-channel-aa-1", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10")

  val newChannelSameCallIDA2 = CallNew("the-uuid-channel-aa-2", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10")


  val endChannelSameCallIDA1 = CallEnd("the-uuid-channel-aa-1", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-2")

  val endChannelSameCallIDA2 = CallEnd("the-uuid-channel-aa-2", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-1")

  "this test should" should {
    " return one item list of BasicACD time series " in {
      within(60000 millis) {
        var acd = 0

        callRouterActor ! newChannelSameCallIDA1
        expectNoMsg(FiniteDuration(1, "seconds"))
        callRouterActor ! newChannelSameCallIDA2
        expectNoMsg(FiniteDuration(1, "seconds"))
        //Thread.sleep(1000)
        callRouterActor ! endChannelSameCallIDA1
        expectNoMsg(FiniteDuration(1, "seconds"))
        callRouterActor ! endChannelSameCallIDA2
        expectNoMsg(FiniteDuration(1, "seconds"))
        //Thread.sleep(12000)

        // trigger the actor to get acd data
        val ACD = "acd"
        basicStatsActor ! ACD
        expectNoMsg(FiniteDuration(1, "seconds"))

        basicStatsActor ! GetBasicAcdTimeSeries
        expectMsgPF() {
          case x@List(ACDTimeSeries(_, _)) =>
            x.headOption match {
              case Some(a) => acd = a.asInstanceOf[ACDTimeSeries].acd
              case None => acd = 10
            }

          case x @ List() => acd = 30

          case _ => acd = 20
        }
// first call billsec is 180 and second is 120 so ACD is (180+120)/2=150
        acd should be (150)
      }
    }

  }


}