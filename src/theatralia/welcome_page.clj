(ns theatralia.welcome-page
  (:require [net.cgrand.enlive-html :as html]
            [garden.core :refer [css]]
            [garden.stylesheet :refer [at-font-face]]
            [clojure.string :as string]
            [clojure.java.io :as io]))
;;; Credits:
;;;  - https://github.com/cgrand/enlive
;;;  - https://github.com/swannodette/enlive-tutorial/
;;;  - http://clojurewebdev.com/workshop/slides/
;;;  - http://www.w3schools.com/css/default.asp

(def ^:private main-html (io/resource "templates/around.html"))
(def ^:private welcome-html (io/resource "templates/welcome.html"))

(html/defsnippet welcome-snippet
  welcome-html
  [:#main]
  [] identity)

(html/defsnippet login-snippet
  welcome-html
  [:#login]
  [] identity)

(html/deftemplate index main-html
  []
  [:title] (html/content "Welcome to Theatralia!")
  [:#main-content] (html/content (welcome-snippet))
  [:#global-tools] (html/content (login-snippet)))

(def css-data [[:html {:font-family "sans-serif"}]
               [:#theatralia {:font-family "fancy"}]
               [:#login {:float "right"}]])
(def ^:private css-map-for-style
  {\R {}
   \B {:font-weight "bold"}
   \I {:font-style "italic"} })

(defn- build-font-css [family-name fonts-path font-basename styles-to-build]
  (map
    (fn [style]
      (let [full-name (str font-basename "_" style ".woff")
            path (str fonts-path "/" full-name)
            style-map (apply merge (map css-map-for-style (seq style)))]
        (at-font-face (merge {:font-family family-name
                              :src (str "url(" path ")")}
                             style-map))))
    styles-to-build))




(def ^:private font-declaration
  (concat
    (build-font-css "my-sans" "/fonts" "LinBiolinum" ["R" "RB" "RI"])
    (build-font-css "my-serif" "/fonts" "LinLibertine" ["R" "RB" "RI" "RBI"])))

(defn main-css []
  {:status 200
   :headers {"Content-Type" "text/css"}
   :body (css css-data)})
