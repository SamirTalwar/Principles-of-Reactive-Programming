package kvstore

import scala.collection.mutable
import akka.actor.{Cancellable, Props, Actor, ActorRef}
import scala.concurrent.duration._

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class ReReplicate(key: String, valueOption: Option[String], id: Long, seq: Long)
  case class Replicated(key: String, id: Long)
  
  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
  
  case class ReplicationContext(id: Long, originator: ActorRef, resender: Cancellable)
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import context.dispatcher

  private var replications = mutable.Map.empty[Long, ReplicationContext]

  /* TODO Behavior for the Replicator. */
  def receive = replicator(0)

  override def postStop() {
    replications.values foreach { case ReplicationContext(_, _, resender) =>
      resender.cancel()
    }
  }

  private def replicator(sequenceCounter: Long): Receive = {
    case Replicate(key, valueOption, id) =>
      replications(sequenceCounter) = ReplicationContext(id, sender, null)
      sendSnapshot(key, valueOption, id, sequenceCounter)
      context.become(replicator(sequenceCounter + 1))

    case ReReplicate(key, valueOption, id, seq) =>
      sendSnapshot(key, valueOption, id, seq)

    case SnapshotAck(key, seq) =>
      val ReplicationContext(id, originator, resender) = replications(seq)
      originator ! Replicated(key, id)
      resender.cancel()

      replications -= seq
  }

  private def sendSnapshot(key: String, valueOption: Option[String], id: Long, seq: Long) {
    val ReplicationContext(id, originator, _) = replications(seq)
    replica ! Snapshot(key, valueOption, seq)
    val cancellable = context.system.scheduler.scheduleOnce(200.milliseconds, self, ReReplicate(key, valueOption, id, seq))
    replications += seq -> ReplicationContext(id, originator, cancellable)
  }
}
