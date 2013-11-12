package quickcheck

import common._

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {

  property("min1") = forAll { a: Int =>
    val h = insert(a, empty)
    findMin(h) == a
  }

  property("inserting another value equal to the minimum value does not change the minimum value") = forAll { heap: H =>
    val min =
      if (isEmpty(heap)) 0
      else findMin(heap)
    findMin(insert(min, heap)) == min
  }

  lazy val genHeap: Gen[H] = Gen.sized { size =>
    for {
      items <- listOfN(size, arbitrary[Int])
    } yield items.foldLeft(empty)((heap, value) => insert(value, heap))
  }

  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)
}
