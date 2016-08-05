// See LICENSE for license details.

package chisel3.iotesters

import chisel3._
import chisel3.testers.CircuitGraph

/**
  * Runs the ClassicTester and returns a Boolean indicating test success or failure
  * @@backendType determines whether the ClassicTester uses verilator or the firrtl interpreter to simulate the circuit
  * Will do intermediate compliation steps to setup the backend specified, including cpp compilation for the verilator backend and firrtl IR compilation for the firrlt backend
  */
object runPeekPokeTester {
  def apply[T <: Module](dutGen: () => T, backendType: String = "firrtl")(testerGen: (T, Option[Backend]) => PeekPokeTester[T]): Boolean = {
    val backend = backendType match {
      case "firrtl" => setupFirrtlTerpBackend(dutGen)
      case "verilator" => setupVerilatorBackend(dutGen)
      case "vcs" => setupVCSBackend(dutGen)
      case _ => throw new Exception("Unrecongnized backend type $backendType")
    }
    val circuitGraph = new CircuitGraph
    val circuit = Driver.elaborate(dutGen)
    val dut = (circuitGraph construct circuit).asInstanceOf[T]
    testerGen(dut, Some(backend)).finish
  }
}

/**
  * Runs the ClassicTester using the verilator backend without doing Verilator compilation and returns a Boolean indicating success or failure
  * Requires the caller to supply path the already compile Verilator binary
  */
object runPeekPokeTesterWithVerilatorBinary {
  def apply[T <: Module] (dutGen: () => T, verilatorBinaryFilePath: String)
                         (testerGen: (T, Option[Backend]) => PeekPokeTester[T]): Boolean = {
    val circuitGraph = new CircuitGraph
    val circuit = Driver.elaborate(dutGen)
    val dut = (circuitGraph construct circuit).asInstanceOf[T]
    val tester = testerGen(dut, Some(new VerilatorBackend(circuitGraph, List(verilatorBinaryFilePath))))
    tester.finish
  }
}

