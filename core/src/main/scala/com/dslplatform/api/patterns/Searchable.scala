package com.dslplatform.api.patterns

/** Domain object that can be queried from the remote server.
  * Server supports custom objects, such as SQL and LINQ objects
  * which are not entities, but can be searched using specifications
  * and other methods
  *
  * DSL example:
  * {{{
  * module Legacy {
  *   sql Town 'SELECT id, name FROM town' {
  *     Int id;
  *     String name;
  *   }
  * }
  * }}}
  */
trait Searchable
