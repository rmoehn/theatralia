(ns theatralia.core
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(println "Hello world!")

(defn foo [a b]
  (* a b))
