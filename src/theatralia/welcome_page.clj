(ns theatralia.welcome-page
  (:require [net.cgrand.enlive-html :as html]
            [garden.core :refer [css]]
            [garden.stylesheet :refer [at-font-face]]
            [garden.units :as u]
            [garden.color :refer [rgb]]
            [clojure.string :as string]
            [clojure.java.io :as io]))
;;; Credits:
;;;  - https://github.com/cgrand/enlive
;;;  - https://github.com/swannodette/enlive-tutorial/
;;;  - http://clojurewebdev.com/workshop/slides/
;;;  - http://stackoverflow.com/questions/15193848/i-want-my-label-to-vertically-align-with-my-input-field
;;;  - http://www.w3schools.com/css/default.asp
;;;  - http://stackoverflow.com/questions/1676891/mapping-a-function-on-the-values-of-a-map-in-clojure

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

;;; The colours come from Ethan Schoonover's Solarized theme:
;;; http://ethanschoonover.com/solarized
(def ^:private color-vec-for
  {:base03    [  0  43  54]
   :base02    [  7  54  66]
   :base01    [ 88 110 117]
   :base00    [101 123 131]
   :base0     [131 148 150]
   :base1     [147 161 161]
   :base2     [238 232 213]
   :base3     [253 246 227]
   :yellow    [181 137   0]
   :orange    [203  75  22]
   :red       [220  50  47]
   :magenta   [211  54 130]
   :violet    [108 113 196]
   :blue      [ 38 139 210]
   :cyan      [ 42 161 152]
   :green     [133 153   0]})

(defn- map-vals [f m]
  (zipmap (keys m) (map f (vals m))))

(def ^:private color (map-vals #(apply rgb %) color-vec-for))

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

(def ^:private defaults
  {:border [[(u/em 0.1) "solid" (color :base1)]]})

(def ^:private css-data
  (concat
    font-declaration
    [[:* {:font-family "my-sans"}]
     [:html {:background-color (color :base3)
             :color (color :base00)}]
     [:header {:background-color (color :base2)
               :color (color :base0)}]
     [:.theatralia {:font-family "my-serif" :font-weight "bold"}]
     [:#main-content {:float "left"}]
     [:p {:max-width (u/ch 65)}]
     [:#global-tools {:float "left"}]
     [:label :input {:display "block"}]
     [:a:link {:color (color :violet)}]
     [:a:visited {:color (color :base1)}]
     [:fieldset {:border-style "none"
                 :background-color (color :base2)}]
     [:input {:background-color "inherit"
              :color (color :base0)
              :border (defaults :border)
              :font-size "inherit"}]
     ["input[type=submit]" {:background-color (color :base3)}]
     [:button {:background-color (color :base2)
               :color (color :base0)
               :border (defaults :border)}]
     [:footer {:clear "both"
               :color (color :base1)
               :text-align "center"
               :font-size "small"
               :padding-top (u/ex 2)}]]))

(defn main-css []
  {:status 200
   :headers {"Content-Type" "text/css"}
   :body (css css-data)})
