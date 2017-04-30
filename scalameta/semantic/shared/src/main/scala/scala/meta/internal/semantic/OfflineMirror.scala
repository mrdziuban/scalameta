package scala.meta
package internal
package semantic

import java.io._
import org.scalameta.data._
import scala.{Seq => _}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta.internal.io.PathIO
import scala.meta.parsers._
import scala.meta.semantic._

@data class OfflineMirror(classpath: String, sourcepath: String) extends Mirror {
  private def failEmpty(what: String) =
    sys.error(
      s"$what must be non-empty. " +
        s"This may indicate that Mirror is badly configured. " +
        s"If you use sbt-scalahost, make sure your project defines " +
        s"`dependsOn(<projectname> % Scalameta)` for at least one <projectname>.")
  if (classpath == null || classpath == "") failEmpty("classpath")
  if (sourcepath == null || sourcepath == "") failEmpty("sourcepath")

  lazy val sources: Seq[Source] = {
    val scalaFiles = mutable.ListBuffer[File]()
    def addFile(file: File): Unit = {
      if (!file.getPath.endsWith(".scala")) return
      scalaFiles += file
    }
    def explore(file: File): Unit = {
      if (file.isDirectory) {
        val files = file.listFiles
        if (files != null) {
          files.filter(_.isFile).foreach(addFile)
          files.filter(_.isDirectory).foreach(explore)
        }
      } else {
        addFile(file)
      }
    }
    val fragments = sourcepath.split(PathIO.pathSeparator).toList
    fragments.foreach(fragment => explore(new File(fragment)))
    scalaFiles.toList.map(_.parse[Source].get)
  }

  lazy val database: Database = {
    Database.fromClasspath(classpath)
  }
}
