package com.dslplatform.api.patterns

/**
 * Olap cube is online analytical processing concept used for extracting business intelligence.
 * At it's core it's just a grouping of data by some dimensions and aggregation
 * of values through facts. Facts can be sum, count, distinct and various others concepts.
 * Cube can be made from various data sources: aggregates, snowflakes, SQL, LINQ, etc...
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Finance {
 *   aggregate Payment {
 *     timestamp createdAt { versioning; }
 *     string account;
 *     money total;
 *     calculated int year from 'it => it.Year';
 *   }
 *
 *   cube&lt;Payment&gt; Analysis {
 *     dimension account;
 *     dimension year;
 *     count createdAt;
 *     sum total;
 *   }
 * }
 * </pre></blockquote>
 */
trait Cube[TSource <: Searchable] {
  val dimensions: Set[String]
  val facts: Set[String]
}
