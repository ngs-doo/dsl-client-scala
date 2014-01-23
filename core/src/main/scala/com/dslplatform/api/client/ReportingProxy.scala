package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.Cube
import com.dslplatform.api.patterns.History
import com.dslplatform.api.patterns.Identifiable
import com.dslplatform.api.patterns.Report
import com.dslplatform.api.patterns.Repository
import com.dslplatform.api.patterns.Searchable
import com.dslplatform.api.patterns.Specification

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Proxy service to reporting operations such as document generation,
 * report population and history lookup.
 * Report should be used to minimize calls to server.
 * <p>
 * It is preferred to use domain patterns instead of this proxy service.
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Todo {
 *   aggregate Task {
 *     timestamp createdAt;
 *     timestamp? finishedAt;
 *   }
 *
 *   report LoadData {
 *     int maxUnfinished;
 *     List&lt;Task&gt; unfinishedTasks 'it => it.finishedAt == null' LIMIT maxUnfinished ORDER BY createdAt;
 *     List&lt;Task&gt; recentlyFinishedTasks 'it => it.finishedAt != null' LIMIT 10 ORDER BY finishedAt DESC;
 *   }
 * }
 * </pre></blockquote>
 */
trait ReportingProxy {
  /**
   * Populate report. Send message to server with serialized report specification.
   *
   * @param report specification
   * @return       future to populated results
   */
  def populate[TResult: ClassTag](report: Report[TResult]): Future[TResult]

  /**
   * Create document from report. Send message to server with serialized report specification.
   * Server will return template populated with found data.
   * <p>
   * DSL example:
   * <blockquote><pre>
   * module Todo {
   *   report LoadData {
   *     List&lt;Task&gt; unfinishedTasks 'it => it.finishedAt == null' ORDER BY createdAt;
   *     templater createDocument 'Tasks.docx' pdf;
   *   }
   * }
   * </pre></blockquote>
   * @param report    report specification
   * @param templater templater name
   * @return          future to document content
   */
  def createReport[TResult](
      report: Report[TResult],
      templater: String): Future[Array[Byte]]

  /**
   * Perform data analysis on specified data source.
   * Data source is filtered using provided specification.
   * Analysis is performed by grouping data by dimensions
   * and aggregating information using specified facts.
   *
   * @param templater     templater report
   * @param specification filter data source
   * @param dimensions    group by dimensions
   * @param facts         analyze using facts
   * @param limit         maximum number of results
   * @param offset        skip initial results
   * @param order         custom order for result
   * @return              future to document content
   */
  def olapCube[TCube <: Cube[TSearchable]: ClassTag, TSearchable <: Searchable : ClassTag](
    templater: String,
      specification: Option[Specification[TSearchable]] = None,
      dimensions: TraversableOnce[String] = Nil,
      facts: TraversableOnce[String] = Nil,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      order: Map[String, Boolean] = Map.empty): Future[Array[Byte]]

  /**
   * Helper method for Olap cube.
   * Perform data analysis on specified data source.
   * Data source is filtered using provided specification.
   * Analysis is performed by grouping data by dimensions
   * and aggregating information using specified facts.
   *
   * @param templater     templater report
   * @param specification filter data source
   * @param dimensions    group by dimensions
   * @param facts         analyze using facts
   * @return              future to document content
   */
  def olapCube[TCube <: Cube[TSearchable]: ClassTag, TSearchable <: Searchable : ClassTag](
    templater: String,
      specification: Specification[TSearchable],
      dimensions: TraversableOnce[String],
      facts: TraversableOnce[String]): Future[Array[Byte]] =
        olapCube(templater, Some(specification), dimensions, facts)

  /**
   * Get aggregate root history.
   * {@link History History} is collection of snapshots made at state changes.
   *
   * @param uris     collection of aggregate identities
   * @return         future to collection of found aggregate histories
   */
  def getHistory[TAggregate <: AggregateRoot: ClassTag](
      uris: TraversableOnce[String]): Future[IndexedSeq[History[TAggregate]]]

  /**
   * Populate template using found domain object.
   * Optionally convert document to PDF.
   *
   * @param file     template file
   * @param uri      domain object identity
   * @param toPdf    convert populated document to PDF
   * @return         future to populated document
   */
  def findTemplater[TIdentifiable <: Identifiable: ClassTag](
      file: String,
      uri: String,
      toPdf: Boolean = true): Future[Array[Byte]]

  /**
   * Populate template using domain objects which satisfies
   * {@link Specification[TSearchable] specification}.
   * Optionally convert document to PDF.
   *
   * @param manifest      domain object type
   * @param file          template file
   * @param specification filter domain objects using specification
   * @param toPdf         convert populated document to PDF
   * @return              future to populated document
   */
  def searchTemplater[TSearchable <: Searchable : ClassTag](
      file: String,
      specification: Option[Specification[TSearchable]] = None,
      toPdf: Boolean = true): Future[Array[Byte]]
}
