// See LICENSE for license details.

package chisel3.iotesters

import chisel3.Module
import chisel3.Driver.createTempDirectory
import scala.util.DynamicVariable
import java.io.File

object Driver {
  private val backendVar = new DynamicVariable[Option[Backend]](None)
  private[iotesters] def backend = backendVar.value

  /**
    * Runs the ClassicTester and returns a Boolean indicating test success or failure
    * @@backendType determines whether the ClassicTester uses verilator or the firrtl interpreter to simulate the circuit
    * Will do intermediate compliation steps to setup the backend specified, including cpp compilation for the verilator backend and firrtl IR compilation for the firrlt backend
    */
  def apply[T <: Module](dutGen: () => T,
                         backendType: String = "firrtl",
                         dir: File = createTempDirectory("iotesters"),
                         debug: Boolean = false)
                         (testerGen: T => PeekPokeTester[T]): Boolean = {
    val (dut, backend) = backendType match {
      case "firrtl" => setupFirrtlTerpBackend(dutGen) // TODO: support dir & debug?
      case "verilator" => setupVerilatorBackend(dutGen, dir, debug)
      case "vcs" => setupVCSBackend(dutGen, dir, debug)
      case _ => throw new Exception("Unrecongnized backend type $backendType")
    }
    backendVar.withValue(Some(backend)) {
      try {
        testerGen(dut).finish
      } catch { case e: Throwable =>
        e.printStackTrace
        TesterProcess.killall
        throw e
      }
    }
  }

  def compile[T <: Module](dutGen: () => T,
                           backendType: String = "firrtl",
                           dir: File = createTempDirectory("iotesters"),
                           debug: Boolean = false): T = {
    val (dut, _) = backendType match {
      case "firrtl" => setupFirrtlTerpBackend(dutGen) // TODO: suport debug & dir?
      case "verilator" => setupVerilatorBackend(dutGen, dir, debug)
      case "vcs" => setupVCSBackend(dutGen, dir, debug)
      case _ => throw new Exception("Unrecongnized backend type $backendType")
    }
    dut
  }

  /**
    * Runs the ClassicTester using the verilator backend without doing Verilator compilation and returns a Boolean indicating success or failure
    * Requires the caller to supply path the already compile Verilator binary
    */
  def run[T <: Module](dutGen: () => T,
                       cmd: Seq[String])
                      (testerGen: T => PeekPokeTester[T]): Boolean = {
    val circuit = chisel3.Driver.elaborate(dutGen)
    val dut = getTopModule(circuit).asInstanceOf[T]
    backendVar.withValue(Some(new VerilatorBackend(dut, cmd))) {
      try {
        testerGen(dut).finish
      } catch { case e: Throwable =>
        e.printStackTrace
        TesterProcess.killall
        throw e
      }
    }
  }

  def run[T <: Module](dutGen: () => T,
                       binary: String,
                       args: String*)
                      (testerGen: T => PeekPokeTester[T]): Boolean =
    run(dutGen, binary +: args.toSeq)(testerGen)

  def run[T <: Module](dutGen: () => T,
                       binary: File,
                       waveform: Option[File] = None,
                       vpdmem: Boolean = false)
                      (testerGen: T => PeekPokeTester[T]): Boolean = {
    val args = (waveform, vpdmem) match {
      case (None, false) => Nil
      case (None, true) => Seq(s"+vpdmem")
      case (Some(f), false) => Seq(s"+waveform=$f")
      case (Some(f), true) => Seq(s"+waveform=$f +vpdmem")
    }
    run(dutGen, binary.toString +: args.toSeq)(testerGen)
  }
}
