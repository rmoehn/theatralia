(ns thomsky.interfaces)

;; TODO: Add clear-handlers!. (Change of name to be consistent with
;; IEventHandlers.)
(defprotocol IQueryBroker
  (register [this key-v handler-fn])
  (subscribe [this v]))

;; TODO: Add purge-chan.
(defprotocol IEventConveyor
  (start! [this])
  (dispatch [this event-v])
  (dispatch-sync [this event-v]))

;; TODO: Add middleware stuff.
;; TODO: Add clear-handlers!.
(defprotocol IEventHandlers
  (register [this event-id handler-fn] [this event-id middlewares handler-fn])
  (handle [this event-v]))
