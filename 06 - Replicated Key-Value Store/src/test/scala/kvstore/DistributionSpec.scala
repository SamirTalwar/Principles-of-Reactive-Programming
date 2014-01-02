package kvstore

import kvstore.Arbiter.{Join, JoinedPrimary, JoinedSecondary, Replicas}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.OptionValues._
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class DistributionSpec extends TestKit(ActorSystem("DistributionSpec"))
  with FunSuite
  with BeforeAndAfterAll
  with ShouldMatchers
  with ImplicitSender
  with Tools {

  test("the Primary distributes to many Secondaries") {
    val arbiter = TestProbe()
    val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.stable), "primary")
    val secondaries = (1 to 1).map(i => system.actorOf(Replica.props(arbiter.ref, Persistence.stable), s"secondary-$i")).toSet
    val replicas = Seq(primary) ++ secondaries
    val primaryClient = session(primary)
    val secondaryClients = secondaries.map(secondary => session(secondary))
    val clients = Seq(primaryClient) ++ secondaryClients

    replicas foreach { _ => arbiter.expectMsg(Join) }
    arbiter.send(primary, JoinedPrimary)
    secondaries foreach { secondary => arbiter.send(secondary, JoinedSecondary) }

    arbiter.send(primary, Replicas(Set(primary) ++ secondaries))

    for (i <- 1 to 20) {
      primaryClient.setAcked(s"key-$i", s"Value $i")
    }

    for (i <- 1 to 20) {
      for (client <- clients) {
        client.get(s"key-$i").value should be (s"Value $i")
      }
    }
  }
}
