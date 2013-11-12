package quickcheck

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
    val min = minOf(heap).getOrElse(0)
    findMin(insert(min, heap)) == min
  }

  property("inserting a value less than the minimum value replaces that as the new minimum value") = forAll { heap: H =>
    val min = minOf(heap).getOrElse(0)
    (min > Int.MinValue) ==> {
      val newMin = min - 1
      findMin(insert(newMin, heap)) == newMin
    }
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

  property("repeatedly finding the minimum results in a sorted sequence") = forAll { heap: H =>
    val heapAsList = toList(heap)
    heapAsList.sorted == heapAsList
  }

  property("melding two heaps results in a minimum of the smaller of the two minimums") = forAll { (a: H, b: H) =>
    (!isEmpty(a) || !isEmpty(b)) ==> {
      val min = Seq(minOf(a), minOf(b)).flatten.min
      val meldedHeap = meld(a, b)
      findMin(meldedHeap) == min
    }
  }

  lazy val genHeap: Gen[H] = Gen.sized { size =>
    for {
      extraItemCount <- choose(0, size)
      items <- listOfN(size + extraItemCount, arbitrary[Int])
    } yield {
      val heapWithExtras = items.foldLeft(empty)((heap, value) => insert(value, heap))
      val heapOfTheCorrectSize = (0 until extraItemCount).foldLeft(heapWithExtras)((heap, _) => deleteMin(heap))
      heapOfTheCorrectSize
    }
  }

  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)

  def minOf(heap: H) =
    if (isEmpty(heap)) None
    else Some(findMin(heap))

  def toList(heap: H): List[Int] =
    if (isEmpty(heap))
      Nil
    else
      findMin(heap) :: toList(deleteMin(heap))
}
