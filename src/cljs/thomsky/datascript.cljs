(ns thomsky.datascript
  (:require [datascript :as d]
            [cljs-uuid-utils.core :as uuid]
            [reagent.core :as reagent]))

;;; Credits:
;;;  - https://gist.github.com/allgress/11348685 (Using it on silent permission.
;;;    – I've asked in two places and they didn't respond. Also it appears not
;;;    to be as fast as it could be, as can be read in
;;;    https://groups.google.com/d/topic/clojurescript/o0W57ptvPc8/discussion.
;;;    But right now I don't understand what's written there. If the UI turns up
;;;    performance problems, I will look into this.)

(defn bind
  ([conn q]
   (bind conn q (reagent/atom nil)))
  ([conn q state]
   (let [k (uuid/make-random-uuid)]
     (reset! state (d/q q @conn))
     (d/listen! conn k (fn [tx-report]
                         (let [novelty (d/q q (:tx-data tx-report))]
                           ;; Only update if query results actually changed.
                           (when (not-empty novelty)
                             (reset! state (d/q q (:db-after tx-report)))))))
     (set! (.-__key state) k)
     state)))

(defn unbind [conn state]
  (d/unlisten! conn (.-__key state)))
