(ns thomsky.core
  (:require [datascript :as d]
            [clojure.core.async :as async]
            [re-frame.utils :as rf-utils]
            [thomsky.subs :as subscriptions]))

;;; Credits:
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/subs.cljs

;;; Notes:
;;;  - I tried to clean up the design. (At least in my opinion it's cleaning up.
;;;  - Doing this I tried not to loose any knowledge that went into re-frame,
;;;    because I can't afford to loose knowledge, because I don't have much
;;;    myself. At least not about front-end stuff.

(defrecord ThomskyApp [app-db query-handler-for event-chan event-handler-for])

(defrecord ThomskyApp [app-db query-broker event-conveyor event-handlers])

(defn make-thomsky-app
  [schema query-broker event-conveyor event-handler]
     (->ThomskyApp (d/create-conn schema)
                   query-broker
                   event-conveyor
                   event-handler))
