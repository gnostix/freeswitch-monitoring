/*
 * Copyright (c) 2015 Alexandros Pappas p_alx hotmail com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

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

 /* val failedCallsActor = system.actorOf(Props(classOf[FailedCallsActor]))

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
  }*/
}

