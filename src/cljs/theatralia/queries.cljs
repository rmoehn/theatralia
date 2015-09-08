(ns theatralia.queries
  "DataScript queries that are needed in more than one place.")

(def tags-query
  "When executed, returns a collection of [index tag] pairs for all tags in the
  database."
  '[:find ?index ?tag
                  :where [?e :tag/s-id ?index]
                         [?e :tag/text ?tag]])
