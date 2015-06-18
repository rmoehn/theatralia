(ns thomsky.subscriptions
  (:require (re-frame.utils :as rf-utils)
            (thomsky.interfaces :as interfaces)))

;;; Credits:
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/subs.cljs

(defrecord QueryBroker [app-db query-handler-for]
  interfaces/IQueryBroker

  (register [this key-v handler-fn]
    (when (contains? query-handler-for key-v)
      (rf-utils/warn "re-frame: overwriting subscription-handler for: " key-v))
    (assoc-in this [:query-handler-for key-v] handler-fn))

  (subscribe [this v]
    (let [key-v (rf-utils/first-in-vector v)]
      (if-some [handler-fn (get event-handler-for key-v)]
        (handler-fn app-db v)
        (rf-utils/error "re-frame: no subscription handler registered for: \""
                        key-v "\".  Returning a nil subscription.")))))

(defn make-query-broker [app-db]
  (->QueryBroker app-db {}))
