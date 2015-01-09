(require
  '[cljs.repl :as repl]
  '[cljs.repl.node :as node])

(repl/repl* (node/repl-env)
  {:output-dir "resources/public/js/out"
   :optimizations :none
   :cache-analysis true
   :source-map true})
