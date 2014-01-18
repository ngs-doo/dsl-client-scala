package com.dslplatform.api.patterns

/**
 * Report is concept for aggregating multiple calls into a single object.
 * By providing search arguments and specifying queries with search predicates,
 * order, limit and offset using LINQ data will be populated on the server.
 * <p>
 * DSL example:
 * <blockquote><pre>
 * module Blog {
 *   aggregate Post {
 *     timestamp createdAt { versioning; }
 *     string author;
 *     string content;
 *   }
 *
 *   report FindPosts {
 *     string? byAuthor;
 *     date? from;
 *     Set&lt;Post&gt; postsFromAuthor 'it => it.author == byAuthor' ORDER BY createdAt;
 *     Array&lt;Task&gt; recentPosts 'it => it.createdAt >= from' LIMIT 20 ORDER BY createdAt DESC;
 *   }
 * }
 * </pre></blockquote>
 */
trait Report[TResult]
