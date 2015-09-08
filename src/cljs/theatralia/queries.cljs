(ns theatralia.queries)

(def tags-query '[:find ?index ?tag
                  :where [?e :tag/s-id ?index]
                         [?e :tag/text ?tag]])
