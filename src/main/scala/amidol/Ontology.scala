package amidol

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.sys.addShutdownHook

import java.io.{Closeable, File}
import java.lang.AutoCloseable
import java.sql.{DriverManager, Statement, PreparedStatement, ResultSet}
import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorSystem
import akka.stream.Materializer

import spray.json._
import java.io.PrintWriter
import scala.collection.mutable
import scala.sys.process._

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

  // The prepared statement is not allowed to leak out... don't let it!
  private def withPreparedStatement[A](sql: String)(
    withPrepStmt: PreparedStatement => A,
  ): A = {
    val preparedStatement = connection.prepareStatement(sql)
    try { withPrepStmt(preparedStatement) } finally { preparedStatement.close() }
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
      implicit class SplitOps(str: String) {
        def safeSplit(sep: Char): Array[String] =
          if (str.isEmpty()) Array.empty else str.split(sep)
      }

      SnomedRecord(
        id = rs.getInt(1),
        code = rs.getInt(2),
        terms = rs.getString(3).safeSplit(';'),
        children = rs.getString(4).safeSplit(',').map(_.toInt),
        parents = rs.getString(5).safeSplit(',').map(_.toInt),
        annotations = rs.getString(6),
      )
    }
  }


  /** Search for records that include a certain search term in their terms */
  def getWithTerm(searchTerm: String, limit: Long): Vector[SnomedRecord] =
    withPreparedStatement("SELECT * FROM snomed WHERE terms LIKE ? LIMIT ?") {
      prepared =>
        prepared.setString(1, "%" + searchTerm + "%")
        prepared.setLong(2, limit)
        val resultSet = prepared.executeQuery()

        val builder = Vector.newBuilder[SnomedRecord]
        while (resultSet.next())
          builder += SnomedRecord.readOffResultSet(resultSet)
        builder.result()
    }

  /** Extract from the DB the record with the given ID */
  def getRecord(id: Int): Option[SnomedRecord] =
    withPreparedStatement("SELECT * FROM snomed WHERE id = ?;") { prepared =>
      prepared.setInt(1, id)
      val resultSet = prepared.executeQuery()

      if (resultSet.next())
        Some(SnomedRecord.readOffResultSet(resultSet))
      else
        None
    }

  /** Travel down the search graph (in breadth first fashion), starting at nodes
   *  with the search term and stopping a recursive search only if the current
   *  record matches a predicate
   */
  def searchForFirst(
    searchTerm: String,
    searchMatch: SnomedRecord => Boolean,
    haltOnMatch: Boolean,
    limit: Long,
    deadline: Deadline,
    createSearchDotImage: Option[String]
  ): Vector[SnomedRecord] = {

    val dotfile = createSearchDotImage.map { _ =>
      val f = File.createTempFile("search_graph", ".dot")
      f.deleteOnExit()
      DotFileVisitor(f, searchTerm)
    }
    val visitor: GraphVisitor = dotfile.getOrElse(GraphVisitor.empty)
    val found = Vector.newBuilder[SnomedRecord]

    // DFS state
    object todo {
      private val visitedIds: mutable.Set[Int] = mutable.Set.empty
      private val todoIds: mutable.Queue[Int] = mutable.Queue.empty

      /** Add an element to visit */
      def enqueue(id: Int): Unit = if (visitedIds.add(id)) todoIds.enqueue(id)

      /** Get the next element to visit */
      def next(): Int = todoIds.dequeue()

      /** Check if we have something to visit */
      def hasNext: Boolean = todoIds.nonEmpty
    }

    // Starting points
    for (record <- getWithTerm(searchTerm, limit)) {
      todo.enqueue(record.id)
      visitor.visitStartingPoint(s"id${record.id}")
    }

    // The search
    var searchLeft = limit
    while (searchLeft > 0 && todo.hasNext && deadline.hasTimeLeft()) {
      searchLeft -= 1
      val id = todo.next()
      val record = getRecord(id).getOrElse {
        throw new Exception(s"Failed to find record with id $id")
      }

      val matches = searchMatch(record)

      val thisId = s"id${record.id}"
      dotfile.foreach { d =>
        val tooltipLines = record.terms.map { term: String =>
          val filtered = term.filterNot("\n\r\t\"".contains(_))
          if (filtered.length > 50) {
            filtered.take(47) + "..."
          } else {
            filtered
          }
        }
        d.addTooltip(thisId, tooltipLines.mkString("\"", "\\n", "\""))
      }
      if (matches) {
        visitor.visitEndingPoint(thisId)
        found += record
      }
      if (!matches || !haltOnMatch) {
        for (parentId <- record.parents) {
          visitor.visitEdge(s"$thisId", s"id$parentId")
          todo.enqueue(parentId)
        }
      }
    }

    while (todo.hasNext) {
      val id = todo.next()
      visitor.visitAborted(s"id$id")
    }

    dotfile.zip(createSearchDotImage).foreach {
      case (dotFileVisitor, imageFileName) =>
        dotFileVisitor.closeDotFile()
        val path = dotFileVisitor.file.getAbsolutePath()
        val exit = (Process(s"dot -Tsvg $path") #> new File(imageFileName)).!
        if (exit != 0) println("Failed to create graph")
    }

    found.result()
  }
}


/** Represents how a search graph can be visited */
sealed trait GraphVisitor { self =>
  def visitStartingPoint(id: String): Unit
  def visitEdge(fromId: String, toId: String): Unit
  def visitEndingPoint(id: String): Unit
  def visitAborted(id: String): Unit

  final def +(other: GraphVisitor): GraphVisitor = new GraphVisitor {
    def visitStartingPoint(id: String): Unit = {
      self.visitStartingPoint(id)
      other.visitStartingPoint(id)
    }

    def visitEdge(fromId: String, toId: String): Unit = {
      self.visitEdge(fromId, toId)
      other.visitEdge(fromId, toId)
    }

    def visitEndingPoint(id: String): Unit = {
      self.visitEndingPoint(id)
      other.visitEndingPoint(id)
    }

    def visitAborted(id: String): Unit = {
      self.visitAborted(id)
      other.visitAborted(id)
    }
  }
}
object GraphVisitor {

  /** Does nothing */
  case object empty extends GraphVisitor {
    def visitStartingPoint(_id: String): Unit = ()
    def visitEdge(_fromId: String, _toId: String): Unit = ()
    def visitEndingPoint(_id: String): Unit = ()
    def visitAborted(id: String): Unit = ()
  }
}


case class DotFileVisitor(file: File, name: String) extends GraphVisitor {
  val pw = new PrintWriter(file)
  pw.println(s"digraph $name {")

  def visitStartingPoint(id: String): Unit =
    pw.println(s"  $id [style=filled,fillcolor=green];")

  def visitEdge(fromId: String, toId: String): Unit =
    pw.println(s"  $fromId -> $toId;")

  def visitEndingPoint(id: String): Unit =
    pw.println(s"  $id [style=filled,fillcolor=red];")

  def visitAborted(id: String): Unit =
    pw.println(s"  $id [shape=diamond];")

  def addTooltip(id: String, tooltip: String): Unit =
    pw.println(s"  $id [tooltip=$tooltip];")

  def closeDotFile(): Unit = {
    pw.println("}")
    pw.close()
  }
}
