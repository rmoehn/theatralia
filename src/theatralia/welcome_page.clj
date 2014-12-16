(ns theatralia.welcome-page
  (:require [net.cgrand.enlive-html :as html]
            [clojure.java.io :as io]))
;;; Credits:
;;;  - https://github.com/cgrand/enlive
;;;  - https://github.com/swannodette/enlive-tutorial/

(def ^:private main-html (io/resource "templates/around.html"))
(def ^:private welcome-html (io/resource "templates/welcome.html"))

(html/defsnippet welcome-snippet
  welcome-html
  [:body :> html/any-node]
  [] identity)

(html/deftemplate index main-html
  []
  [:#main-content] (html/content (welcome-snippet)))
