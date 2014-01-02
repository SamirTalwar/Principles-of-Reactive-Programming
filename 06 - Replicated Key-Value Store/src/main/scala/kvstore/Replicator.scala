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
  
  case class ReplicationContext(id: Long, originator: ActorRef, schedule: Cancellable)
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import context.dispatcher

  private var replications = mutable.Map.empty[Long, ReplicationContext]

  /* TODO Behavior for the Replicator. */
  def receive = replicator(0)

  override def postStop() {
    replications.values foreach { case ReplicationContext(_, _, schedule) =>
      schedule.cancel()
    }
  }

  private def replicator(sequenceCounter: Long): Receive = {
    case Replicate(key, valueOption, id) =>
      val schedule = context.system.scheduler.schedule(0.milliseconds, 100.milliseconds, self, ReReplicate(key, valueOption, id, sequenceCounter))
      replications(sequenceCounter) = ReplicationContext(id, sender, schedule)
      context.become(replicator(sequenceCounter + 1))

    case ReReplicate(key, valueOption, id, seq) =>
      replica ! Snapshot(key, valueOption, seq)

    case SnapshotAck(key, seq) =>
      val ReplicationContext(id, originator, schedule) = replications(seq)
      originator ! Replicated(key, id)
      schedule.cancel()

      replications -= seq
  }
}
