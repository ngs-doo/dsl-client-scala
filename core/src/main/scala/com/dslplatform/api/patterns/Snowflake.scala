package com.dslplatform.api.patterns

/** Snowflake schema is an concept from OLAP used for building
  * specific query projection. Join rules follow few basic principals
  * such as single starting aggregate (entity), navigation through
  * references/relationships. maintaining 1:1 relationship with
  * starting entity, etc.
  * Snowflake is a read-only projection and can be viewed as a view in
  * the database spanning related objects.
  *
  * Lazy load can be avoided by including all related information
  * onto the snowflake and fetching them all at once.
  *
  * DSL example:
  * {{{
  * module Todo {
  *   aggregate Task {
  *     DateTime startedAt;
  *     DateTime? finishedAt;
  *     Int? priority;
  *     User *user;
  *   }
  *   aggregate User {
  *     String name;
  *     String email;
  *   }
  *   snowflake&lt;Task&gt; TaskList {
  *     startedAt;
  *     finishedAt;
  *     priority;
  *     user.name;
  *     user.email as userEmail;
  *     order by priority asc, startedAt desc;
  *   }
  * }
  * }}}
  *
  */
trait Snowflake[TAggregate <: AggregateRoot]
    extends Identifiable
