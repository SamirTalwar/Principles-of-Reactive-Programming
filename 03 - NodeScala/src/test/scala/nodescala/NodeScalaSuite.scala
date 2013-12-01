package nodescala

import scala.language.postfixOps
import scala.collection._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.async.Async.async
import org.scalatest._
import NodeScala._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeScalaSuite extends FunSuite {

  test("A Future should always be created") {
    val always = Future.always(517)

    assert(Await.result(always, 0 nanos) == 517)
  }

  test("A Future should never be created") {
    val never = Future.never[Int]

    try {
      Await.result(never, 1 second)
      fail()
    } catch {
      case t: TimeoutException => // ok!
    }
  }

  test("`Future.delay` stalls for the time duration specified") {
    val delayed = Future.delay(500 milliseconds)
    Await.result(delayed, 600 milliseconds)
  }

  test("`all` succeeds when every future succeeds") {
    val a = Future.delay(100 milliseconds).map(_ => 3)
    val b = Future.delay(200 milliseconds).map(_ => 6)
    val c = Future.delay(300 milliseconds).map(_ => 9)
    val all = Future.all(List(a, b, c))
    assert(Await.result(all, 500 milliseconds) == List(3, 6, 9))
  }

  test("`all` fails when any single future fails") {
    val error = new OutOfMemoryError("Nope.")

    val a = Future.delay(100 milliseconds).map(_ => 3)
    val b = Future.failed(error)
    val c = Future.delay(300 milliseconds).map(_ => 9)
    val all = Future.all(List(a, b, c))

    try {
      Await.result(all, 500 milliseconds)
      fail()
    } catch {
      case e: ExecutionException => assert(e.getCause == error)
    }
  }

  test("`any` succeeds when its first future to complete succeeds") {
    val a = Future.delay(300 milliseconds).flatMap(_ => Future.failed(new Exception))
    val b = Future.delay(100 milliseconds).map(_ => "Woop.")
    val c = Future.delay(200 milliseconds).flatMap(_ => Future.failed(new Exception))
    val any = Future.any(List(a, b, c))
    assert(Await.result(any, 500 milliseconds) == "Woop.")
  }

  test("`any` fails when its first future to complete fails") {
    val exception = new Exception
    val a = Future.delay(300 milliseconds).flatMap(_ => Future.failed(new Exception))
    val b = Future.delay(200 milliseconds).map(_ => "Woop.")
    val c = Future.delay(100 milliseconds).flatMap(_ => Future.failed(exception))
    val any = Future.any(List(a, b, c))

    try {
      Await.result(any, 500 milliseconds)
      fail()
    } catch {
      case e: Exception => assert(e == exception)
    }
  }

  test("CancellationTokenSource should allow stopping the computation") {
    val cts = CancellationTokenSource()
    val ct = cts.cancellationToken
    val p = Promise[String]()

    async {
      while (ct.nonCancelled) {
        // do work
      }

      p.success("done")
    }

    cts.unsubscribe()
    assert(Await.result(p.future, 1 second) == "done")
  }

  class DummyExchange(val request: Request) extends Exchange {
    @volatile var response = ""
    val loaded = Promise[String]()
    def write(s: String) {
      response += s
    }
    def close() {
      loaded.success(response)
    }
  }

  class DummyListener(val port: Int, val relativePath: String) extends NodeScala.Listener {
    self =>

    @volatile private var started = false
    var handler: Exchange => Unit = null

    def createContext(h: Exchange => Unit) = this.synchronized {
      assert(started, "is server started?")
      handler = h
    }

    def removeContext() = this.synchronized {
      assert(started, "is server started?")
      handler = null
    }

    def start() = self.synchronized {
      started = true
      new Subscription {
        def unsubscribe() = self.synchronized {
          started = false
        }
      }
    }

    def emit(req: Request) = {
      val exchange = new DummyExchange(req)
      if (handler != null) handler(exchange)
      exchange
    }
  }

  class DummyServer(val port: Int) extends NodeScala {
    self =>
    val listeners = mutable.Map[String, DummyListener]()

    def createListener(relativePath: String) = {
      val l = new DummyListener(port, relativePath)
      listeners(relativePath) = l
      l
    }

    def emit(relativePath: String, req: Request) = this.synchronized {
      val l = listeners(relativePath)
      l.emit(req)
    }
  }

  test("Listener should serve the next request as a future") {
    val dummy = new DummyListener(8191, "/test")
    val subscription = dummy.start()

    def test(req: Request) {
      val f = dummy.nextRequest()
      dummy.emit(req)
      val (reqReturned, xchg) = Await.result(f, 1 second)

      assert(reqReturned == req)
    }

    test(immutable.Map("StrangeHeader" -> List("StrangeValue1")))
    test(immutable.Map("StrangeHeader" -> List("StrangeValue2")))

    subscription.unsubscribe()
  }

  test("Server should serve requests") {
    val dummy = new DummyServer(8191)
    val dummySubscription = dummy.start("/testDir") {
      request => for (kv <- request.iterator) yield (kv + "\n").toString
    }

    // wait until server is really installed
    Thread.sleep(500)

    def test(req: Request) {
      val webpage = dummy.emit("/testDir", req)
      val content = Await.result(webpage.loaded.future, 1 second)
      val expected = (for (kv <- req.iterator) yield (kv + "\n").toString).mkString
      assert(content == expected, s"'$content' vs. '$expected'")
    }

    test(immutable.Map("StrangeRequest" -> List("Does it work?")))
    test(immutable.Map("StrangeRequest" -> List("It works!")))
    test(immutable.Map("WorksForThree" -> List("Always works. Trust me.")))

    dummySubscription.unsubscribe()
  }

}




