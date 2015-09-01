(ns theatralia.queries)

(def tags-query '[:find ?index ?tag
                  :where [?e :tag/index ?index]
                         [?e :tag/content ?tag]])

