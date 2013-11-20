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

    verifyDemultiplexer(input, control, output,
      (false, List()) -> List(false),
      (true, List()) -> List(true)
    )
  }

  test("a demultiplexer with one control bit is a fork") {
    val input = new Wire
    val control = List(new Wire)
    val output = List(new Wire, new Wire)
    demux(input, control, output)

    verifyDemultiplexer(input, control, output,
      (false, List(false)) -> List(false, false),
      (false, List(true )) -> List(false, false),
      (true,  List(false)) -> List(false, true ),
      (true,  List(true )) -> List(true,  false)
    )
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

  private def verifyDemultiplexer(input: Wire, control: List[Wire], output: List[Wire], expectations: ((Boolean, List[Boolean]), List[Boolean])*) {
    expectations foreach { case ((inputValue, controlValues), outputValues) =>
      input.setSignal(inputValue)
      control.zip(controlValues).foreach { case (controlWire, controlValue) => controlWire.setSignal(controlValue) }
      run
      assert(output.map(_.getSignal) === outputValues)
    }
  }
}
