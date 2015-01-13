(ns theatralia.core
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;;; Credits:
;;;  - https://github.com/swannodette/om
;;;  - https://github.com/swannodette/om/wiki/Basic-Tutorial

(enable-console-print!)

(println "Hello world!")

(defn handle-change [event owner {:keys [text]}]
  (om/set-state! owner :text (.. event -target -value)))

(defn process-input [owner]
  (let [input (.-value (om/get-node owner "misc-input"))]
    (when input
      (println (str "Input: " input))
      (om/set-state! owner :text ""))))

(defn widget [data owner]
  (reify
    om/IInitState
    (init-state [_] {:text ""})

    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h1 nil (:headline data))
        (dom/input
          #js {:type "text"
               :ref "misc-input"
               :value (:text state)
               :onChange #(handle-change % owner state)
               :onKeyDown #(when (= (.-key %) "Enter")
                                 (process-input owner))})
        (dom/button
          #js {:onClick #(process-input owner)}
          "Send")))))

(om/root widget {:headline "Hello world!"}
         {:target (. js/document (getElementById "app"))})
