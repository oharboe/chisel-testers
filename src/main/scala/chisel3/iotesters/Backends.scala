// See LICENSE for license details.
package chisel3.iotesters

import chisel3.internal.InstanceId
import java.io.PrintStream

/**
  * define interface for ClassicTester backend implementations such as verilator and firrtl interpreter
  */

private[iotesters] abstract class Backend(val logger: PrintStream, _seed: Long = System.currentTimeMillis) {
  val rnd = new scala.util.Random(_seed)

  def poke(signal: InstanceId, value: BigInt, off: Option[Int])
          (implicit verbose: Boolean, base: Int): Unit

  def peek(signal: InstanceId, off: Option[Int])
          (implicit verbose: Boolean, base: Int): BigInt

  def poke(path: String, value: BigInt)
          (implicit verbose: Boolean, base: Int): Unit

  def peek(path: String)
          (implicit verbose: Boolean, base: Int): BigInt

  def expect(signal: InstanceId, expected: BigInt)
            (implicit verbose: Boolean, base: Int): Boolean =
    expect(signal, expected, "")

  def expect(signal: InstanceId, expected: BigInt, msg: => String)
            (implicit verbose: Boolean, base: Int): Boolean

  def expect(path: String, expected: BigInt)
            (implicit verbose: Boolean, base: Int): Boolean =
    expect(path, expected, "")

  def expect(path: String, expected: BigInt, msg: => String)
            (implicit verbose: Boolean, base: Int): Boolean

  def step(n: Int): Unit

  def reset(n: Int): Unit

  def finish: Unit
}


