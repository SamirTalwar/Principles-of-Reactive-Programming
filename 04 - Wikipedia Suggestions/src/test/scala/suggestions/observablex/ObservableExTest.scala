package suggestions.observablex

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class ObservableExTest extends FunSuite with ShouldMatchers {
  test("an observable is created from a future") {
    val observable = ObservableEx(Future("thing"))
    val observed = mutable.Buffer[String]()
    observable subscribe {
      observed += _
    }

    observed should be (Seq("thing"))
  }
}
