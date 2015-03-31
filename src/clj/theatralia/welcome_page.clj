(ns theatralia.welcome-page
  "Functions for assembling the Welcome page with Enlive templating and Garden
  CSS."
  (:require [net.cgrand.enlive-html :as html]
            [garden.core :refer [css]]
            [garden.units :as u]
            [garden.color :refer [rgb]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [theatralia.style.fonts :refer [font-declaration]]))
;;; Credits:
;;;  - https://github.com/cgrand/enlive
;;;  - https://github.com/swannodette/enlive-tutorial/
;;;  - http://clojurewebdev.com/workshop/slides/
;;;  - http://stackoverflow.com/questions/15193848/i-want-my-label-to-vertically-align-with-my-input-field
;;;  - http://www.w3schools.com/css/default.asp
;;;  - http://stackoverflow.com/questions/1676891/mapping-a-function-on-the-values-of-a-map-in-clojure
;;;  - http://philipwalton.github.io/solved-by-flexbox/

;;;; Assembling HTML from templates

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

;;;; Assembling CSS from data structures.

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
   :accent    [203  75  22]
   :red       [220  50  47]
   :magenta   [211  54 130]
   :violet    [108 113 196]
   :blue      [ 38 139 210]
   :cyan      [ 42 161 152]
   :green     [133 153   0]})

;; TODO: Use medley.core/map-vals.
(defn- map-vals [f m]
  (zipmap (keys m) (map f (vals m))))

(def ^:private color (map-vals #(apply rgb %) color-vec-for))

(def ^:private defaults
  {:border [[(u/em 0.1) "solid" (color :base1)]]})

(def ^:private css-data
  (concat
    (font-declaration :theano-didot)
    (font-declaration :chivo)
    [[:* {:font-family "my-sans"}]
     [:html {:background-color (color :base3)
             :color (color :base00)}]
     [:header {:background-color (color :base2)
               :color (color :base0)
               :padding (u/em 0.5)
               }]
     [:body {:display "flex"
             :min-height (u/vh 100)
             :margin 0
             :flex-direction "column"}]
     [:main {:display "flex"
             :flex 1
             :flex-direction "row"
             :justify-content "center"}]
     [:#home {:font-family "my-serif"
              :font-size "large"
              :color (color :accent)}]
     [:h1 {:font-family "my-serif"
           :font-weight "normal"
           :font-size "xx-large"
           :color (color :accent)}]
     [:#main-content {:margin (u/em 0.4)}]
     [:p {:max-width (u/ch 62)}]
     [:#global-tools {:margin-top (u/em 3)
                      :margin-left (u/em 2)}]
     [:.stack {:display "flex"
               :align-items "center"
               :flex-direction "column"}]
     [:label :input {:display "block"}]
     [:a:link {:color (color :accent)}]
     [:a:visited {:color (color :accent)}]
     [:fieldset {:border-style "none"
                 :background-color (color :base2)}]
     [:input {:background-color "inherit"
              :color (color :base0)
              :border (defaults :border)
              :font-size "inherit"}]
     ["input[type=submit]" {:background-color (color :base3)}]
     [:button {:background-color (color :base2)
               :color (color :base0)
               :font-size "inherit"
               :width "10em"
               :border (defaults :border)}]
     [:footer {;:clear "both"
               :color (color :base1)
               :text-align "center"
               :padding (u/em 0.5)
               :font-size "small"}]]))

(defn main-css []
  {:status 200
   :headers {"Content-Type" "text/css"}
   :body (css css-data)})
