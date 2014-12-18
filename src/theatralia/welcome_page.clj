(ns theatralia.welcome-page
  (:require [net.cgrand.enlive-html :as html]
            [garden.core :refer [css]]
            [clojure.java.io :as io]))
;;; Credits:
;;;  - https://github.com/cgrand/enlive
;;;  - https://github.com/swannodette/enlive-tutorial/
;;;  - http://clojurewebdev.com/workshop/slides/

(def ^:private main-html (io/resource "templates/around.html"))
(def ^:private welcome-html (io/resource "templates/welcome.html"))

(html/defsnippet welcome-snippet
  welcome-html
  [:body :> html/any-node]
  [] identity)

(html/deftemplate index main-html
  []
  [:title] (html/content "Welcome to Theatralia!")
  [:#main-content] (html/content (welcome-snippet)))

(def css-data [[:html {:font-family "sans-serif"}]
               [:#theatralia {:font-family "fancy"}]
               [:#login {:float "right"}]])

(defn main-css []
  {:status 200
   :headers {"Content-Type" "text/css"}
   :body (css css-data)})
