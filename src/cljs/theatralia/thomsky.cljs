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
;;;  - https://gist.github.com/allgress/11348685, though it isn't complete. See
;;;    the comments. See here for a bit more discussion:
;;;    https://groups.google.com/d/topic/clojurescript/o0W57ptvPc8/discussion
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/middleware.cljs
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/core.cljs

;; Note:
;;
;; If the application is getting slow, your first step towards a diagnosis
;; should be to uncomment the line below and watch in the console when and how
;; often bind is called. – If it only gets called as many times as there are
;; binds in the code and only on application load, your problem lies somewhere
;; else. If it gets called more often than that and while the application runs
;; (for example, when you click or type), there are two likely causes:
;;
;;  - You've wrapped a call to bind in a reagent.ratom/reaction. In this case
;;    bind gets called whenever the app-db changes, which is not what you want.
;;
;;  - You've used a Form-1 component where you should use a Form-2 component.
;;    See https://github.com/Day8/re-frame/wiki/Creating-Reagent-Components.
;;
;; If it wasn't repeated bind calls, you might want to exclude scratch changes
;; from being checked for novelties. At tag filter-scratch you can find a
;; commit that introduced that, but wasn't needed at the time.
;;
;; TODO: When all the querying gets too slow, we have to introduce some
;;       cleverness. See the links in the credits above and
;;       https://github.com/tonsky/datascript/pull/12. (RM 2015-09-17)
(defn bind
  "Returns a ratom containing the result of query Q with the arguments Q-ARGS on
  the value of the database behind CONN."
  [q conn & q-args]
  ;(println "bind called" q) ; Commented out on purpose. – See note above.
  (let [k (uuid/make-random-uuid)
        state (reagent/atom nil)
        res (apply d/q q @conn q-args)]
    (reset! state res)
    (d/listen! conn k
               (fn [tx-report]
;                 (println tx-report)
                 (let [new-result (apply d/q q (:db-after tx-report) q-args)]
                   (when (not= new-result @state)
                     (reset! state new-result)))))
    (set! (.-__key state) k)
    state))

(defn unbind
  "Stops changes in CONN from causing updates of ratom STATE."
  [conn state]
  (d/unlisten! conn (.-__key state)))

(defn pure-datascript
  "Adaptation of the re-frame middleware 'pure' for Thomsky.

  The HANDLER wrapped in this middleware will receive the current value behind
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
  "Handler registration procedure that provides the right defaults for
  Theatralia.

  In re-frame, the default handler registration procedure applies only the
  'pure' middleware to a handler. Here we apply the respective 'pure-datascript'
  middleware as well as the 'trim-v' middleware, since we never need this silly
  keyword in our subscription handlers anyway."
  ([id handler] (register-handler id [] handler))
  ([id middleware handler]
   (handlers/register-base id
                           [pure-datascript middleware middleware/trim-v]
                           handler)))
