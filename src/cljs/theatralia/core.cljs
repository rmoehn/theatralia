(ns theatralia.core
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;;; Credits: https://github.com/swannodette/om

(enable-console-print!)

(println "Hello world!")

(defn widget [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil (:text data)))))

(om/root widget {:text "Hello world!"}
         {:target (. js/document (getElementById "app"))})
