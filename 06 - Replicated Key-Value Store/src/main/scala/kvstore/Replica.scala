package kvstore

import scala.collection.mutable
import akka.actor._
import kvstore.Arbiter._
import scala.collection.immutable.Queue
import akka.actor.SupervisorStrategy.Restart
import scala.annotation.tailrec
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import akka.util.Timeout

object Replica {
  sealed trait Operation {
    def key: String
    def id: Long
  }
  case class Insert(key: String, value: String, id: Long) extends Operation
  case class Remove(key: String, id: Long) extends Operation
  case class Get(key: String, id: Long) extends Operation

  case class RePersist(key: String, valueOption: Option[String], seq: Long)

  sealed trait OperationReply
  case class OperationAck(id: Long) extends OperationReply
  case class OperationFailed(id: Long) extends OperationReply
  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))

  case class PersistenceContext(originator: ActorRef, repersister: Cancellable)
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {
  import Replica._
  import Replicator._
  import Persistence._
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */
  
  val kv = mutable.Map.empty[String, String]
  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]

  val persistence = context.actorOf(persistenceProps)
  val persistMessages = mutable.Map.empty[Long, PersistenceContext]

  arbiter ! Join

  def receive = {
    case JoinedPrimary   => context.become(leader)
    case JoinedSecondary => context.become(replica(0))
  }

  /* TODO Behavior for  the leader role. */
  private def leader: Receive = _leader orElse persistenceHandler((key, id) => OperationAck(id))

  private def _leader: Receive = {
    case Get(key, id) =>
      sender ! GetResult(key, kv.get(key), id)

    case Insert(key, value, id) =>
      kv += (key -> value)
      persist(key, Some(value), id)

    case Remove(key, id) =>
      kv -= key
      persist(key, None, id)
  }

  private def replica(expectedSeq: Long) = _replica(expectedSeq) orElse persistenceHandler(SnapshotAck)

  private def _replica(expectedSeq: Long): Receive = {
    case Get(key, id) =>
      sender ! GetResult(key, kv.get(key), id)

    case Snapshot(key, valueOption, seq) =>
      if (seq < expectedSeq) {
        sender ! SnapshotAck(key, seq)
      } else if (seq == expectedSeq) {
        if (valueOption.isDefined)
          kv += (key -> valueOption.get)
        else
          kv -= key

        context.become(replica(expectedSeq + 1))

        persist(key, valueOption, seq)
      }
  }

  def persistenceHandler(acknowledgement: (String, Long) => Any): Receive = {
    case RePersist(key, valueOption, id) =>
      persist(key, valueOption, id)

    case Persisted(key, id) =>
      val PersistenceContext(originator, repersister) = persistMessages(id)
      originator ! acknowledgement(key, id)
      repersister.cancel()
      persistMessages -= id
  }

  private def persist(key: String, valueOption: Option[String], id: Long) {
    persistence ! Persist(key, valueOption, id)
    val originator = persistMessages.get(id).map(_.originator).getOrElse(sender)
    val cancellable = context.system.scheduler.scheduleOnce(100.milliseconds, self, RePersist(key, valueOption, id))
    persistMessages += id -> PersistenceContext(originator, cancellable)
  }
}
