(ns theatralia.thomsky
  (:require [datascript :as d]
            datascript.core
            [cljs-uuid-utils.core :as uuid]
            [re-frame.core :as rf]
            [re-frame.handlers :as handlers]
            [re-frame.middleware :as middleware]
            [plumbing.core :refer [safe-get]]
            [reagent.core :as reagent]))

(defn set-up-datascript!
  "Swaps in a Datascript database with schema ?SCHEMA (default: Datascript
  default schema) for the APP-DB in re-frame. To be used as an event handler."
  ([app-db [_ & [?schema]]]
   (let [conn (d/create-conn (or ?schema datascript.core/default-schema))]
     (reset! app-db @conn)
     (reset-meta! app-db (meta conn)))))

;;; Credits:
;;;  - https://gist.github.com/allgress/11348685, see here for a bit more
;;;    discussion:
;;;    https://groups.google.com/d/topic/clojurescript/o0W57ptvPc8/discussion
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/middleware.cljs
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/core.cljs

(defn bind
  "Returns a ratom containing the result of query Q on the value of the database
  behind CONN. Pass STATE if you want to use an existing ratom."
  [q conn & q-args]
  (let [k (uuid/make-random-uuid)
        state (reagent/atom nil)
        res (apply d/q q @conn q-args)]
    (reset! state res)
    (d/listen!
      conn
      k
      (fn [tx-report]
        (let [tx-data (:tx-data tx-report)
              scratch-change? (d/q '[:find ?e .
                                     :where [?e :scratch/val _]]
                                   tx-data)]
          (when-not scratch-change?
            (println "Wasn't scratch change: " tx-data)
            (let [novelty (apply d/q q (:tx-data tx-report) q-args)]
              ;; Only update if query results actually changed.
              (when (not-empty novelty)
                (reset! state (apply d/q q (:db-after tx-report) q-args))))))))
    (set! (.-__key state) k)
    state))

(defn unbind
  "Stops updates on ratom STATE from changes in CONN."
  [conn state]
  (d/unlisten! conn (.-__key state)))

(defn pure-datascript
  "Adaptation of the re-frame middleware 'pure' for Thomsky.

  The HANDLER wrapped with this middleware will receive the current value behind
  CONN and has to return a Datascript transaction data structure that will be
  transact!ed over CONN.

  So, as opposed to re-frame's 'pure', where the HANDLER has to return a whole
  new app state, here the HANDLER only returns transaction data that will be
  used to change the app state."
  [handler]
  (fn pure-datascript-handler [conn event-vec]
    (assert (satisfies? cljs.core/IAtom conn)
            (str "conn has to be an atom. Got: " conn))
    (let [db @conn
          _ (assert (satisfies? datascript.core.IDB db)
                    (str "@conn has to be a Datascript Database. Got: " db))
          txd (handler db event-vec)]
      (assert (sequential? txd)
              (str "Handler has to return sequence of transaction data."
                   "Got: " txd))
      (d/transact! conn txd))))

(defn register-handler
  "Handler registration procedure that provides the right defaults for Theatralia.

  In re-frame, the default handler registration procedure applies only the
  'pure' middleware to a handler. Here we apply the respective 'pure-datascript'
  middleware as well as the 'trim-v' middleware, since we never need this silly
  keyword in our subscription handlers anyway."
  ([id handler] (register-handler id [] handler))
  ([id middleware handler]
   (handlers/register-base id
                           [pure-datascript middleware/trim-v middleware]
                           handler)))
