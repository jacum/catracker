package nl.pragmasoft.catracker

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, TestInbox}
import akka.actor.typed.ActorRef
import nl.pragmasoft.catracker.Model.StoredPosition
import nl.pragmasoft.catracker.Tracker.State
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.FixtureAnyWordSpec
import org.scalatest.{BeforeAndAfterAll, Outcome}

class TrackerActorSpec extends FixtureAnyWordSpec
  with BeforeAndAfterAll
  with Matchers
  with Eventually {

  case class FixtureParam(listener: ActorRef[State])
  private val testKit = ActorTestKit()

//  def withFixture(test: OneArgTest): Outcome = {
//    withFixture(test.toNoArgTest(FixtureParam(listener.ref)))
//  }

  protected def withFixture(test: OneArgTest): Outcome = ???
  override def afterAll(): Unit = testKit.shutdownTestKit()

//  "tracker" should {
//
//    "start tracking of the device" in { ctx =>
//      val trackers = testKit.spawn(Trackers(), "trackers")
//      trackers ! Trackers.Track("device1")
//      testKit.expectEffectType[Spawned[Tracker.Command]]
//    }
//
//    "perform tracking of the device" in { ctx =>
//      val testKit = BehaviorTestKit(Tracker("sensor1", ctx.listener))
//      testKit.run(Tracker.Update(StoredPosition(
//        1,
//        System.currentTimeMillis(),
//        "app",
//        "type",
//        "serial",
//        1.1,
//        2.2,
//        true,
//        "gw",
//        3.3,
//        100,
//        20,
//        1
//      )))
//      val clientInbox = TestInbox[Tracker.Tell]()
//
//      clientInbox.expectMessage(Tracker.Tell(ctx.listener))
//
////      val probe = testKit.createTestProbe[Tracker.Pong]()
//    }

//    "accumulate positions and send them back" in {

//      val pinger = testKit.spawn(Tracker("device", listener), "device")
//      val probe = testKit.createTestProbe[Tracker.Pong]()
//      pinger ! Echo.Ping("hello", probe.ref)
//      probe.expectMessage(Echo.Pong("hello"))
//    }
//  }

}
