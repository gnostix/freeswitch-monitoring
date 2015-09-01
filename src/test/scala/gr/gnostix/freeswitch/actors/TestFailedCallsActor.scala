package gr.gnostix.freeswitch.actors

import gr.gnostix.freeswitch.actors.ActorsProtocol.{GetTotalFailedCalls, GetFailedCalls}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActors, DefaultTimeout, ImplicitSender, TestKit}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by rebel on 24/8/15.
 */
class TestFailedCallsActor
extends TestKit(ActorSystem("TestKitUsageSpec"))
with DefaultTimeout with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val failedCallsActor = system.actorOf(Props(classOf[FailedCallsActor]))

  "this test should" should {
    " return the list of Calls " in {
      within(5000 millis) {
        failedCallsActor ! GetFailedCalls
        expectMsg(List())
      }
    }
  }


  "this is  for number of failed calls" should {
    " return an Int of calls " in {
      within(5000 millis) {
        failedCallsActor ! GetTotalFailedCalls
        expectMsg(TotalFailedCalls(0))
      }
    }
  }
}

