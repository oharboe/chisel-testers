// See LICENSE for license details.
package chisel3.iotesters

import java.io.{File, PrintStream}

import chisel3.{ChiselExecutionResult, Module, Bits}
import chisel3.internal.InstanceId

import scala.collection.mutable.HashMap

import firrtl_interpreter.{InterpreterOptions, InterpretiveTester}

private[iotesters] class FirrtlTerpBackend(dut: Module,
                                           firrtlIR: String,
                                           _seed: Long = System.currentTimeMillis,
                                           interpreterOptions: InterpreterOptions = new InterpreterOptions
                                          ) extends Backend(_seed) {
  val interpretiveTester = new InterpretiveTester(firrtlIR, interpreterOptions)
  reset(5) // reset firrtl interpreter on construction

  val portNames = getDataNames("io", dut.io).toMap

  def poke(signal: InstanceId, value: BigInt, off: Option[Int])
          (implicit logger: PrintStream, verbose: Boolean, base: Int): Unit = {
    signal match {
      case port: Bits =>
        val name = portNames(port)
        interpretiveTester.poke(name, value)
        if (verbose) logger println s"  POKE $name <- ${bigIntToStr(value, base)}"
      case _ =>
    }
  }

  def peek(signal: InstanceId, off: Option[Int])
          (implicit logger: PrintStream, verbose: Boolean, base: Int): BigInt = {
    signal match {
      case port: Bits =>
        val name = portNames(port)
        val result = interpretiveTester.peek(name)
        if (verbose) logger println s"  PEEK $name -> ${bigIntToStr(result, base)}"
        result
      case _ => BigInt(rnd.nextInt)
    }
  }

  def expect(signal: InstanceId, expected: BigInt, msg: => String)
            (implicit logger: PrintStream, verbose: Boolean, base: Int) : Boolean = {
    signal match {
      case port: Bits =>
        val name = portNames(port)
        val got = interpretiveTester.peek(name)
        val good = got == expected
        if (verbose) logger println
           s"""$msg  EXPECT $name -> ${bigIntToStr(got, base)} == ${bigIntToStr(expected, base)}""" +
           s"""${if (good) "PASS" else "FAIL"}"""
        good
      case _ => false
    }
  }

  def poke(path: String, value: BigInt)
          (implicit logger: PrintStream, verbose: Boolean, base: Int): Unit = {
    assert(false)
  }

  def peek(path: String)
          (implicit logger: PrintStream, verbose: Boolean, base: Int): BigInt = {
    assert(false)
    BigInt(rnd.nextInt)
  }

  def expect(path: String, expected: BigInt, msg: => String)
            (implicit logger: PrintStream, verbose: Boolean, base: Int) : Boolean = {
    assert(false)
    false
  }

  def step(n: Int)(implicit logger: PrintStream): Unit = {
    interpretiveTester.step(n)
  }

  def reset(n: Int = 1): Unit = {
    interpretiveTester.poke("reset", 1)
    interpretiveTester.step(n)
    interpretiveTester.poke("reset", 0)
  }

  def finish(implicit logger: PrintStream): Unit = {
    interpretiveTester.report()
  }
}

private[iotesters] object setupFirrtlTerpBackend {
  def apply[T <: chisel3.Module](
                                  dutGen: () => T,
                                  testerOptions: TesterOptions = new TesterOptions,
                                  interpreterOptions: InterpreterOptions = new InterpreterOptions()
                                ): (T, Backend) = {

    chisel3.Driver.execute(testerOptions, dutGen) match {
      case ChiselExecutionResult(Some(circuit), firrtlText, Some(firrtlExecutionResult), true) =>
        val dut = getTopModule(circuit).asInstanceOf[T]
        (dut, new FirrtlTerpBackend(dut, chisel3.Driver.emit(dutGen), interpreterOptions = interpreterOptions))
      case _ =>
        throw new Exception("Problem with compilation")
    }
//
//
//    val circuit = chisel3.Driver.elaborate(dutGen)
//    val dut = getTopModule(circuit).asInstanceOf[T]
//    val dir = new File(s"test_run_dir/${dut.getClass.getName}") ; dir.mkdirs()
//
//    // Dump FIRRTL for debugging
//    chisel3.Driver.dumpFirrtl(circuit, Some(new File(dir, s"${circuit.name}.fir")))
//    (dut, new FirrtlTerpBackend(dut, chisel3.Driver.emit(dutGen)))
  }
}
