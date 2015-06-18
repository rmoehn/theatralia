(ns thomsky.subs)

;;; Credits:
;;;  - https://github.com/Day8/re-frame/blob/master/src/re_frame/subs.cljs

;; TODO: Add clean-handlers!
(defprotocol IQueryBroker
  (register-sub [this key-v handler-fn])
  (subscribe [this v]))
