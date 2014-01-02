package kvstore

import scala.collection.mutable
import akka.actor.{Cancellable, Props, Actor, ActorRef}
import scala.concurrent.duration._

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class ReReplicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)
  
  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
  
  case class ReplicationContext(originator: ActorRef, resender: Cancellable)
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import context.dispatcher

  // A bimap type would be quite handy here.
  private var idToSeq = mutable.Map.empty[Long, Long]
  private var seqToId = mutable.Map.empty[Long, Long]
  private var replications = mutable.Map.empty[Long, ReplicationContext]

  /* TODO Behavior for the Replicator. */
  def receive = replicator(0)

  override def postStop() {
    replications.values foreach { case ReplicationContext(_, resender) =>
      resender.cancel()
    }
  }

  private def replicator(sequenceCounter: Long): Receive = {
    case Replicate(key, valueOption, id) =>
      sendSnapshot(key, valueOption, id, sequenceCounter)
      idToSeq += id -> sequenceCounter
      seqToId += sequenceCounter -> id
      context.become(replicator(sequenceCounter + 1))

    case ReReplicate(key, valueOption, id) =>
      val seq = idToSeq(id)
      sendSnapshot(key, valueOption, id, seq)

    case SnapshotAck(key, seq) =>
      val id = seqToId(seq)
      val ReplicationContext(originator, resender) = replications(seq)
      originator ! Replicated(key, id)
      resender.cancel()
      replications -= seq
      idToSeq -= id
      seqToId -= seq
  }

  private def sendSnapshot(key: String, valueOption: Option[String], id: Long, sequenceCounter: Long) {
    val ReplicationContext(originator, _) = replications.getOrElse(sequenceCounter, ReplicationContext(sender, null))
    replica ! Snapshot(key, valueOption, sequenceCounter)
    val cancellable = context.system.scheduler.scheduleOnce(200.milliseconds, self, ReReplicate(key, valueOption, id))
    replications += sequenceCounter -> ReplicationContext(originator, cancellable)
  }
}
