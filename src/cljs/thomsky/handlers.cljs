(ns thomsky.handlers
  (:refer [thomsky.interfaces :as thy]
          [re-frame.utils :as rf-utils]))

;; TODO: Add the check for middleware factories once we have middleware
;; factories.
(defn comp-middlewares [middlewares]
  {:pre [(sequential? middlewares)]}
  (apply comp middlewares))

(defn report-handler-in-handler [outer-event-v]
  (fn [this inner-event-v]
    (rf-utils/error "re-frame: while handling \"" outer-event-v "\""
                    " dispatch-sync was called on \"" inner-event-v "\"."
                    " You can't call dispatch-sync in an event handler.")))

(defrecord EventHandlers [app-db event-handler-for]
  thy/IEventHandlers

  (register [this event-id handler-fn]
    (register this event-id [] handler-fn))

  (register [this event-id middlewares handler-fn]
    (when (contains? event-handler-for event-id)
      (warn "re-frame: overwriting an event-handler for: " event-id))
    (assoc event-handler-for
           event-id
           (if (empty? middlewares)
             handler-fn
             ((comp-middlewares middlewares) handler-fn))))

  (handle [this event-v]
    (let [key-v (rf-utils/first-in-vector event-v)]
     (if-some [handler-fn (get event-handler-for key-v)]
       (with-redefs [handle (report-handler-in-handler event-v)]
         (handler-fn app-db event-v))))))
