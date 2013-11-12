package quickcheck

import common._

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {
  property("the minimum of a heap with one value is that value") = forAll { value: Int =>
    val heap = insert(value, empty)
    findMin(heap) == value
  }

  property("the minimum of a heap with two values is the smaller") = forAll { (a: Int, b: Int) =>
    val heap = insert(b, insert(a, empty))
    if (a < b)
      findMin(heap) == a
    else
      findMin(heap) == b
  }
  
  property("inserting another value equal to the minimum value does not change the minimum value") = forAll { heap: H =>
    val min =
      if (isEmpty(heap)) 0
      else findMin(heap)
    findMin(insert(min, heap)) == min
  }

  property("inserting a value less than the minimum value replaces that as the new minimum value") = forAll { heap: H =>
    val min =
      if (isEmpty(heap)) 0
      else findMin(heap)
    val newMin = min - 1
    (min >= 0) ==> (findMin(insert(newMin, heap)) == newMin)
  }

  property("deleting the minimum value from a heap with one element results in an empty heap") = forAll { value: Int =>
    val heapWithValue = insert(value, empty)
    val emptiedHeap = deleteMin(heapWithValue)
    isEmpty(emptiedHeap)
  }

  property("deleting the minimum value from a heap with many elements yields a minimum value that is equal or greater") = forAll { heap: H =>
    (!isEmpty(heap)) ==> {
      val min = findMin(heap)
      val newHeap = deleteMin(heap)
      isEmpty(newHeap) || findMin(newHeap) >= min
    }
  }

  lazy val genHeap: Gen[H] = Gen.sized { size =>
    for {
      items <- listOfN(size, posNum[Int])
    } yield items.foldLeft(empty)((heap, value) => insert(value, heap))
  }

  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)
}
