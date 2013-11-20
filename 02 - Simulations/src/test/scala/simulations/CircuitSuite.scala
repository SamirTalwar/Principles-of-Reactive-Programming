package simulations

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CircuitSuite extends CircuitSimulator with FunSuite {
  val InverterDelay = 1
  val AndGateDelay = 3
  val OrGateDelay = 5
  
  test("andGate example") {
    val in1, in2, out = new Wire
    andGate(in1, in2, out)
    in1.setSignal(false)
    in2.setSignal(false)
    run
    
    assert(out.getSignal === false, "and 1")

    in1.setSignal(true)
    run
    
    assert(out.getSignal === false, "and 2")

    in2.setSignal(true)
    run
    
    assert(out.getSignal === true, "and 3")
  }

  test("OR gate") {
    val in1, in2, out = new Wire
    orGate(in1, in2, out)

    testOrGate(in1, in2, out)
  }

  test("OR gate implemented in terms of an AND gate") {
    val in1, in2, out = new Wire
    orGate2(in1, in2, out)

    testOrGate(in1, in2, out)
  }

  test("a demultiplexer with no control bits is a pipe") {
    val input = new Wire
    val control = List()
    val output = List(new Wire)
    demux(input, control, output)

    input.setSignal(false)
    run
    assert(output.map(_.getSignal) === List(false))

    input.setSignal(true)
    run
    assert(output.map(_.getSignal) === List(true))
  }

  private def testOrGate(in1: Wire, in2: Wire, out: Wire) {
    in1.setSignal(false)
    in2.setSignal(false)
    run
    assert(out.getSignal === false, "false | false == false")

    in1.setSignal(true)
    run
    assert(out.getSignal === true, "true | false == true")

    in2.setSignal(true)
    run
    assert(out.getSignal === true, "true | true == true")

    in1.setSignal(false)
    run
    assert(out.getSignal === true, "false | true == true")
  }
}
