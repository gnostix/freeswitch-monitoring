package gr.gnostix.freeswitch.actors

import java.sql.Timestamp

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import gr.gnostix.freeswitch.actors.ActorsProtocol._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

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
        basicStatsActor ! GetBasicStatsTimeSeries
        expectMsg(List())
      }
    }
  }


  "this test should" should {
    " return a  list of size 1 with BasicStats " in {
      within(30000 millis) {
        Thread.sleep(12000)
        var basicStatsSize = 0
        basicStatsActor ! GetBasicStatsTimeSeries
        expectMsgPF() {
          case x@List(BasicStatsTimeSeries(_, _, _, _, _, _, _)) => basicStatsSize = x.size
          case _ => false
        }
        basicStatsSize should be(1)
      }
    }
  }

  val newChannelDiffCallIdA1 = CallNew("the-uuid-channel-a-1", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "the-uuid-channel-a-1", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10", "OUTBOUND", 2, 3)

  val newChannelDiffCallIdA2 = CallNew("the-uuid-channel-a-2", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "the-uuid-channel-a-1", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10", "INBOUND", 2, 3)


  val endChannelDiffCallIdA1 = CallEnd("the-uuid-channel-a-1", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "the-uuid-channel-a-1", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 180, 100, "the-uuid-channel-a-2", "send_bye", "OUTBOUND", 2,3,3)

  val endChannelDiffCallIdA2 = CallEnd("the-uuid-channel-a-2", "CHANNEL_ANSWER", "5003", "5004", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid-other", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 180, 100, "the-uuid-channel-a-1", "recv_bye", "OUTBOUND", 2,3,3)

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
          case x@GetCallsResponse(_, _) =>
            println("totalCalls: " + x.toString)
            totalCalls = x.totalCalls
          case _ => totalCalls = -1
        }

        totalCalls should be(1)
      }
    }
  }

  val newChannelSameCallIDA1 = CallNew("the-uuid-channel-aa-1", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10", "OUTBOUND", 2, 3)

  val newChannelSameCallIDA2 = CallNew("the-uuid-channel-aa-2", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), "Alex-Freeswitch", "10.10.10.10", "INBOUND", 2, 3)


  val endChannelSameCallIDA1 = CallEnd("the-uuid-channel-aa-1", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-2", "send_bye", "OUTBOUND", 2,3,3)

  val endChannelSameCallIDA2 = CallEnd("the-uuid-channel-aa-2", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid", Some(new Timestamp(System.currentTimeMillis())),
    Some(new Timestamp(System.currentTimeMillis())), new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-1", "recv_bye", "INBOUND", 2,3,3)

  "this test should" should {
    " return acd = 2.5 min " in {
      within(60000 millis) {
        var acd = 300d / 2 / 60 //300 sec / 2 calls /60sec = 2.5 min

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
        val BasicStatsTick = "BasicStatsTick"
        basicStatsActor ! BasicStatsTick
        expectNoMsg(FiniteDuration(1, "seconds"))

        basicStatsActor ! GetBasicStatsTimeSeries
        expectMsgPF() {
          case x@List(BasicStatsTimeSeries(_, _, _, _, _, _, _),
          BasicStatsTimeSeries(_, _, _, _, _, _, _)) =>
            x.headOption match {
              case Some(a) => acd = a.asInstanceOf[BasicStatsTimeSeries].acd
              case None => acd = 10
            }
            println("-------------> acd" + x.toString)

          case x =>
            println("-------------> " + x.toString)
            acd = -20
        }
        // first call billsec is 180 and second is 120 so ACD is (180+120)/2=150
        acd should be(2.5)
      }
    }

  }

  // failed call actor tests

  "this test should" should {
    " return the list of Calls " in {
      within(5000 millis) {
        callRouterActor ! GetFailedCalls
        expectMsg(List())
      }
    }
  }


  "this is  for number of failed calls" should {
    " return an Int of calls " in {
      within(5000 millis) {
        callRouterActor ! GetTotalFailedCalls
        expectMsg(TotalFailedCalls(0))
      }
    }
  }

  val endChannelFailedCall = CallEnd("the-uuid-channel-aa-154545", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid-9898988", None,
    None, new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-2767676", "send_bye", "OUTBOUND", 2,3,3)

  "this test should" should {
    " return one item list of FailedCalls " in {
      within(60000 millis) {

        var failedCallsNum = 0
        callRouterActor ! endChannelFailedCall
        expectNoMsg(FiniteDuration(1, "seconds"))

        callRouterActor ! GetFailedCalls
        expectMsgPF() {
          case x@List(CallEnd(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) => failedCallsNum = x.size
          case _ =>
        }
        failedCallsNum should be(1)

      }
    }
  }

  val endChannelFailedCall2 = CallEnd("the-uuid-channel-aa-154545111", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid-989811988", None,
    None, new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-276117676", "send_bye", "OUTBOUND", 2,3,3)
  "this test should" should {
    " return total FailedCalls " in {
      within(60000 millis) {

        var failedCallsNum = 0
        callRouterActor ! endChannelFailedCall2
        expectNoMsg(FiniteDuration(1, "seconds"))

        callRouterActor ! GetTotalFailedCalls
        expectMsgPF() {
          case x@TotalFailedCalls(a) => failedCallsNum = a
          case _ =>
        }
        failedCallsNum should be(2) // since is the second failed call we are pushing..

      }
    }
  }

  val endChannelFailedCall3 = CallEnd("the-uuid-channel-aa-000121200000", "CHANNEL_ANSWER", "5001", "5002", "GSM", "GSM",
    "192.168.100.101", "192.168.100.102", "call-uuid-98989121288", None,
    None, new Timestamp(System.currentTimeMillis()),
    "Alex-Freeswitch", "10.10.10.10", "NORMAL_CLEARING", 120, 100, "the-uuid-channel-a-27121267676", "send_bye", "OUTBOUND", 2,3,3)

  "this test should" should {
    " return the correct asr 40 " +
      "since we have so far 4 total calls and 2 successfull calls asr= 2 / 5 * 100 " in {
      within(60000 millis) {

        var asr = 0d
        callRouterActor ! endChannelFailedCall3
        expectNoMsg(FiniteDuration(100, "milliseconds"))

        // trigger the actor to get acd data
        val BasicStatsTick = "BasicStatsTick"
        basicStatsActor ! BasicStatsTick
        expectNoMsg(FiniteDuration(100, "milliseconds"))

        basicStatsActor ! GetBasicStatsTimeSeries

        expectMsgPF() {
          case x@List(BasicStatsTimeSeries(_, _, _, _, _, _, _),
          BasicStatsTimeSeries(_, _, _, _, _, _, _),
          BasicStatsTimeSeries(_, _, _, _, _, _, _)) =>
            x headOption match {
              case Some(a) => asr = a.asInstanceOf[BasicStatsTimeSeries].asr
              case None => asr = -10
            }
            println("-----------------> basicStats asr List: " + x)

          case x =>
            println("-----------------> basicStats asr List: " + x)
            asr = -20
          /*            x.asInstanceOf[List[BasicStatsTimeSeries]] headOption match {
                        case Some(a) => asr = a.asInstanceOf[BasicStatsTimeSeries].asr
                        case None => asr = -10
                      }*/
        }

        asr should be(25) // since we have so far 4 total calls and 2 successfull calls asr= 2 / 5 * 100
      }
    }
  }


}