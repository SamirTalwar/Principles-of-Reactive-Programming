package kvstore

import scala.util.Random
import akka.actor.{Props, Actor}
import akka.event.LoggingReceive

object Persistence {
  case class Persist(key: String, valueOption: Option[String], id: Long)
  case class Persisted(key: String, id: Long)

  class PersistenceException extends Exception("Persistence failure")

  def stable: Props = Props(new Persistence)
  def flaky: Props = Props(new FlakyPersistence(stable))
}

class Persistence extends Actor {
  import Persistence._

  def receive = LoggingReceive {
    case Persist(key, _, id) =>
      sender ! Persisted(key, id)
  }
}

class FlakyPersistence(persistenceProps: Props) extends Actor {
  import Persistence._
  
  val persistence = context.actorOf(persistenceProps)

  def receive = LoggingReceive {
    case message @ Persist(key, _, id) =>
      if (Random.nextBoolean()) persistence.tell(message, sender)
      else throw new PersistenceException
  }
}
