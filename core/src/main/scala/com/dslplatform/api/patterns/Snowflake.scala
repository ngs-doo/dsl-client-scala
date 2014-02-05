package com.dslplatform.api.patterns

/**
 * Snowflake schema is an concept from OLAP used for building
 * specific query projection. Join rules follow few basic principals
 * such as single starting aggregate (entity), navigation through
 * references/relationships. maintaining 1:1 relationship with
 * starting entity, etc.
 * Snowflake is a read-only projection and can be viewed as a view in
 * the database spanning related objects.
 * <p>
 * Lazy load can be avoided by including all related information
 * onto the snowflake and fetching them all at once.
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Todo {
 *   aggregate Task {
 *     timestamp startedAt;
 *     timestamp? finishedAt;
 *     int? priority;
 *     User *user;
 *   }
 *   aggregate User {
 *     string name;
 *     string email;
 *   }
 *   snowflake<Task> TaskList {
 *     startedAt;
 *     finishedAt;
 *     priority;
 *     user.name;
 *     user.email as userEmail;
 *     order by priority asc, startedAt desc;
 *   }
 * }
 * </pre></blockquote>
 *
 */
trait Snowflake[TAggregate <: AggregateRoot]
    extends Identifiable
