package com.dslplatform.api.patterns

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Service for creating documents based on templates and data.
 * Data can be provided or specification can be sent so data is queried
 * on the server.
 * <p>
 * Byte array is returned from the server which represents docx, xlsx,
 * text or converted pdf file.
 * <p>
 * More info about Templater library can be found at http://templater.info/
 */
trait TemplaterService {

  /**
   * Returns a document generated from template named {@code file}
   * populated with {@code aggregate}.
   *
   * @param file      template document
   * @param aggregate data to populate with
   * @return          document content future
   */
  def populate[TIdentifiable <: Identifiable](
      file: String,
      aggregate: TIdentifiable): Future[Array[Byte]]

  /**
   * Returns a document generated from template named {@code file}
   * populated with {@code aggregate} and converted to PDF format.
   *
   * @param file      template document
   * @param aggregate data to populate with
   * @return          document content future
   */
  def populatePdf[TIdentifiable <: Identifiable](
      file: String,
      aggregate: TIdentifiable): Future[Array[Byte]]

  /**
   * Returns a document generated from template named {@code file}
   * populated with data which satisfies {@link Specification[TRoot] search predicate}.
   *
   * @param file          template document
   * @param specification search predicate
   * @return              document content future
   */
  def populate[TSearchable <: Searchable: ClassTag](
      file: String,
      specification: Option[Specification[TSearchable]]): Future[Array[Byte]]

  /**
   * Returns a document generated from template named {@code file}
   * populated with data described with {@link Specification[TRoot] search predicate}
   * and converted to PDF format.
   *
   * @param file          template document
   * @param specification search predicate
   * @return              document content future
   */
  def populatePdf[TSearchable <: Searchable: ClassTag](
      file: String,
      specification: Option[Specification[TSearchable]]): Future[Array[Byte]]
}
