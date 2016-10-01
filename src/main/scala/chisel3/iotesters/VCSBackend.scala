// See LICENSE for license details.
package chisel3.iotesters

import scala.collection.mutable.HashMap
import scala.util.Random
import java.io.{File, Writer, FileWriter, PrintStream, IOException}
import java.nio.file.{FileAlreadyExistsException, Files, Paths}
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
  * Copies the necessary header files used for vcs compilation to the specified destination folder
  */
object copyVpiFiles {
  def apply(destinationDirPath: String): Unit = {
    new File(destinationDirPath).mkdirs()
    val simApiHFilePath = Paths.get(destinationDirPath + "/sim_api.h")
    val vpiHFilePath = Paths.get(destinationDirPath + "/vpi.h")
    val vpiCppFilePath = Paths.get(destinationDirPath + "/vpi.cpp")
    val vpiTabFilePath = Paths.get(destinationDirPath + "/vpi.tab")
    val vcsTestFilePath = Paths.get(destinationDirPath + "/vcs_test.v")
    try {
      Files.createFile(simApiHFilePath)
      Files.createFile(vpiHFilePath)
      Files.createFile(vpiCppFilePath)
      Files.createFile(vpiTabFilePath)
      Files.createFile(vcsTestFilePath)
    } catch {
      case x: FileAlreadyExistsException =>
        System.out.format("")
      case x: IOException => {
        System.err.format("createFile error: %s%n", x)
      }
    }

    Files.copy(getClass.getResourceAsStream("/sim_api.h"), simApiHFilePath, REPLACE_EXISTING)
    Files.copy(getClass.getResourceAsStream("/vpi.h"), vpiHFilePath, REPLACE_EXISTING)
    Files.copy(getClass.getResourceAsStream("/vpi.cpp"), vpiCppFilePath, REPLACE_EXISTING)
    Files.copy(getClass.getResourceAsStream("/vpi.tab"), vpiTabFilePath, REPLACE_EXISTING)
    Files.copy(getClass.getResourceAsStream("/vcs_test.v"), vcsTestFilePath, REPLACE_EXISTING)
  }
}

private[iotesters] object setupVCSBackend {
  def apply[T <: chisel3.Module](dutGen: () => T, dir: File): (T, Backend) = {
    val circuit = chisel3.Driver.elaborate(dutGen)
    val dut = getTopModule(circuit).asInstanceOf[T]

    // Generate CHIRRTL
    val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(dutGen))

    // Generate Verilog
    val verilogFile = new File(dir, s"${circuit.name}.v")
    val verilogWriter = new FileWriter(verilogFile)
    val annotation = new firrtl.Annotations.AnnotationMap(Seq(
      new firrtl.passes.InferReadWriteAnnotation(circuit.name, firrtl.Annotations.TransID(-1))))
    (new firrtl.VerilogCompiler).compile(chirrtl, annotation, verilogWriter)
    verilogWriter.close

    // Generate Harness
    val testHarnessFileName = s"${circuit.name}-harness.v"
    val testHarnessFile = new File(dir, testHarnessFileName)
    copyVpiFiles(dir.toString)
    genVerilogHarness(dut, new FileWriter(testHarnessFile))
    assert(verilogToVCS(circuit.name, dir, new File(testHarnessFileName)).! == 0)

    (dut, new VCSBackend(dut, Seq((new File(dir, circuit.name)).toString)))
  }
}

private[iotesters] class VCSBackend(dut: chisel3.Module,
                                    cmd: Seq[String],
                                    _seed: Long = System.currentTimeMillis)
           extends VerilatorBackend(dut, cmd, _seed)
