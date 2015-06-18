(ns thomsky.router
  (:require-macros  [cljs.core.async.macros :as async.macros])
  (:require [thomsky.interfaces :as thy]
            [thomsky.handlers :as handlers]
            [cljs.core.async :as asyc]
            [reagent.core :as reagent]
            [re-frame.utils :as rf-utils]))

;;; Credits:
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/router.cljs



(defrecord EventConveyer [event-chan event-handlers]
  thy/IEventConveyor

  ;; TODO: Add the purge-chan as soon as it gets into a core.async release. (For
  ;; now it doesn't matter since the channel is unbuffered and therefore there
  ;; are no pending events to be purged.) Also add the third note about just
  ;; that.
  ;;
  ;; Adapted notes from re-frame about when the handler throws an exception:
  ;;
  ;;  - Rethrow/allow the exception to bubble up because the app, in production,
  ;;    may have hooked window.onerror and perform special processing.
  ;;
  ;;  - But an exception which bubbles up will break the enclosing go-loop. So
  ;;    we'll need to start another one.
  (start! [this]
    (async.macros/go-loop []
      (let [event-v (async/<! event-chan)]
        (if (:flush-dom (meta event-v))
          (do
            (reagent/flush)
            ;; Wait just over one annimation frame (16ms), to rensure all pending
            ;; GUI work is flushed to the DOM.
            (async/<! (async/timeout 20)))
          ;; Just in case we are handling one dispatch after an other, give the
          ;; browser back control to do its stuff.
          ;; TODO: Encapsulate cleverness.
          (async/<! (async/timeout 0)))
        (try
          (thy/handle event-handlers event-v)
          (catch js/Object e
            (do
              (start! this)
              (throw e)))))
      (recur)))

  (dispatch [this event-v]
    (if (nil? event-v)
      (rf-utils/error "re-frame: \"dispatch\" is ignoring a nil event.")
        ; nil would close the channel.
      (async/put! event-chan event-v))
    nil)

  (dispatch-sync [this event-v]
    (thy/handle event-handlers event-v)
    nil))
