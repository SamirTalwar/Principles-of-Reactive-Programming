package kvstore

import kvstore.Arbiter.{JoinedPrimary, Join}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.OptionValues._
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class TroubledPersistenceSpec extends TestKit(ActorSystem("TroubledPersistenceSpec"))
  with FunSuite
  with BeforeAndAfterAll
  with ShouldMatchers
  with ImplicitSender
  with Tools {
  
  test("a broken persistence layer won't stop a Primary") {
    val arbiter = TestProbe()
    val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.props(flaky = true)), "primary")
    val client = session(primary)

    arbiter.expectMsg(Join)
    arbiter.send(primary, JoinedPrimary)

    for (i <- 1 to 20) {
      client.setAcked(s"key-$i", s"Value $i")
    }

    for (i <- 1 to 20) {
      client.get(s"key-$i").value should be (s"Value $i")
    }
  }
}
