package kvstore

import scala.collection.mutable
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)
  
  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import Replica._
  import context.dispatcher

  var idToSeq = mutable.Map.empty[Long, Long]
  var acks = mutable.Set.empty[Long]

  /* TODO Behavior for the Replicator. */
  def receive = replicator(0)

  def replicator(sequenceCounter: Long): Receive = {
    case message @ Replicate(key, valueOption, id) =>
      val newReplication = !idToSeq.contains(id)
      if (newReplication) {
        replica ! Snapshot(key, valueOption, sequenceCounter)
        context.system.scheduler.scheduleOnce(200.milliseconds, self, message)
        idToSeq += id -> sequenceCounter
        context.become(replicator(sequenceCounter + 1))
      } else {
        val seq = idToSeq(id)
        if (acks.contains(seq)) {
          idToSeq -= id
        } else {
          replica ! Snapshot(key, valueOption, seq)
          context.system.scheduler.scheduleOnce(200.milliseconds, self, message)
        }
      }
    case SnapshotAck(key, seq) =>
      acks += seq
  }

}
