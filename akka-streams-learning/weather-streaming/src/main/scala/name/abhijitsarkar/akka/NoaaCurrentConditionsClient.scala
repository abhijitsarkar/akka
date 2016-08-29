package name.abhijitsarkar.akka

import java.io.File
import java.net.URL
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.{CREATE, WRITE}
import java.nio.file.{Files, StandardOpenOption}
import java.util.zip.ZipFile

/**
  * @author Abhijit Sarkar
  */
object NoaaCurrentConditionsClient {
  val basedir = new File(System.getProperty("java.io.tmpdir"), "current_conditions")

  def currentConditionsPath(overwrite: Boolean = false): String = {
    if ((basedir.exists()) && !overwrite) return basedir.getAbsolutePath

    basedir.delete()
    basedir.mkdirs()
    val zipFile = download
    extract(zipFile)
    zipFile.delete()

    basedir.getAbsolutePath
  }

  private def download = {
    val url = new URL("http://w1.weather.gov/xml/current_obs/all_xml.zip")
    val conn = url.openConnection()
    conn.setReadTimeout(10 * 1000) // 10 s

    val dest = new File(basedir, "cc.zip").toPath
    val os = Files.newOutputStream(dest, CREATE, WRITE)

    val buffer = new Array[Byte](4096)
    val is = conn.getInputStream
    Stream.continually(is.read(buffer))
      .takeWhile(_ != -1)
      .foreach(bytesRead => os.write(buffer, 0, bytesRead))

    is.close()

    dest.toFile
  }

  import collection.JavaConverters._

  private def extract(src: File) = {
    val zipFile = new ZipFile(src)
    val entries = zipFile.entries.asScala
    entries.foreach { e => e match {
      case _ if (e.isDirectory) => new File(basedir, e.getName).mkdirs()
      case _ => Files.copy(zipFile.getInputStream(e), new File(basedir, e.getName).toPath, REPLACE_EXISTING)
    }
    }

    zipFile.close()
  }
}
