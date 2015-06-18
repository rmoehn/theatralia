(ns thomsky.core
  (:require [datascript :as d]
            [re-frame.utils :as rf-utils]
            [thomsky.subs :as subscriptions]))

;;; Credits:
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/subs.cljs

(defrecord ThomskyApp [app-db key->fn]

  subscriptions/IQueryBroker

  (register-sub [this key-v handler-fn]
    (when (contains? key->fn key-v)
      (rf-utils/warn "re-frame: overwriting subscription-handler for: " key-v))
    (assoc-in this [:key->fn key-v] handler-fn))

  (subscribe [this v]
    (let [key-v (rf-utils/first-in-vector v)]
      (if-some [handler-fn (get key->fn key-v)]
        (handler-fn app-db v)
        (rf-utils/error "re-frame: no subscription handler registered for: \""
                        key-v "\".  Returning a nil subscription.")))))

(defn make-thomsky-app
  ([] (make-thomsky-app {}))
  ([schema]
   (map->ThomskyApp {:app-db (d/create-conn schema)
                     :key->fn {}})))
