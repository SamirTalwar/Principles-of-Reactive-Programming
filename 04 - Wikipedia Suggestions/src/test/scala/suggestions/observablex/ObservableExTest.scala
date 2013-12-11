package suggestions.observablex

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import rx.lang.scala.concurrency.TestScheduler

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class ObservableExTest extends FunSuite with ShouldMatchers {
  test("an observable is created from a future") {
    val observable = ObservableEx(Future("thing"))
    val observed = mutable.Buffer[String]()
    val scheduler = TestScheduler()
    observable.subscribe({
      observed += _
    }, { _ =>
      fail("There should never be an 'error' case.")
    }, { () => {
      observed should have (size (1))
    } }, scheduler)

    scheduler.triggerActions()

    observed should be (Seq("thing"))
  }

  test("the observable fails when the future fails") {
    val expectedThrowable = new Exception("Nope.")
    val observable = ObservableEx(Future.failed[Int](expectedThrowable))
    val scheduler = TestScheduler()
    var passed = false
    observable.subscribe({ _: Int =>
      fail("There should never be a 'next'.")
    }, { throwable =>
      throwable should be (expectedThrowable)
      passed = true
    }, { () => {
      fail("There should never be a 'completed'.")
    } }, scheduler)

    scheduler.triggerActions()

    passed should be (true)
  }
}
