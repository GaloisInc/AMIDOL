package amidol

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.sys.addShutdownHook

import java.io.{Closeable, File}
import java.lang.AutoCloseable
import java.sql.{DriverManager, Statement, ResultSet}
import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorSystem
import akka.stream.Materializer

import spray.json._

object OntologyDb {

  // Set up the DB
  val dbPath: String = "ontology/AMIDOL-SNOMED.sqlite3"
  private val connection = DriverManager.getConnection(s"jdbc:sqlite:$dbPath")
  addShutdownHook {
    connection.close()
    println("Closed SNOWMED database")
  }

  // The statement is not allowed to leak out... don't let it!
  private def withStatement[A](withStmt: Statement => A): A = {
    val statement = connection.createStatement()
    try { withStmt(statement) } finally { statement.close() }
  }

  // Just testing...
  val snomedSize = withStatement { stmt =>
    val resultSet = stmt.executeQuery("SELECT count(*) FROM snomed")
    resultSet.getLong(1)
  }
  println(s"Loaded SNOMED database ($snomedSize records)")

  // A record in the SNOMED DB
  case class SnomedRecord(
    id: Int,
    code: Int,
    terms: Array[String],
    children: Array[Int],
    parents: Array[Int],
    annotations: String,
  )
  object SnomedRecord {
    def readOffResultSet(rs: ResultSet): SnomedRecord = {
      SnomedRecord(
        id = rs.getInt(1),
        code = rs.getInt(2),
        terms = rs.getString(3).split(';'),
        children = rs.getString(4).split(',').map(_.toInt),
        parents = rs.getString(5).split(',').map(_.toInt),
        annotations = rs.getString(6),
      )
    }
  }
}
