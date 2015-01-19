(ns theatralia.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]))

;;; Credits:
;;;  - https://github.com/swannodette/om
;;;  - https://github.com/swannodette/om/wiki/Basic-Tutorial

(enable-console-print!)

(def app-state
  (atom {}))

(defn handle-change [event owner {:keys [text]}]
  (om/set-state! owner :text (.. event -target -value)))

(defn process-input [owner last-input]
  (let [input (.-value (om/get-node owner "misc-input"))]
    (when input
      (om/transact! last-input :text (fn [_] input) :update)
      (om/set-state! owner :text ""))))

(defn io-view [last-input owner]
  (reify
    om/IInitState
    (init-state [_] {:text ""})

    om/IRenderState
    (render-state [this local-state]
      (dom/div nil
        (dom/h1 nil (:text last-input))
        (dom/input
          #js {:type "text"
               :ref "misc-input"
               :value (:text local-state)
               :onChange #(handle-change % owner local-state)
               :onKeyDown #(when (= (.-key %) "Enter")
                                 (process-input owner last-input))})
        (dom/button
          #js {:onClick #(process-input owner last-input)}
          "Send")))

    om/IDidMount
    (did-mount [_]
      (.focus (om/get-node owner "misc-input")))))

(defn app-view [app owner]
  (do
    (reify
      om/IRender
      (render [_]
        (om/build
          om-sync
          (:last-input app)
          {:opts
           {:view io-view
            :filter (comp #{:update} tx-tag)
            :on-success (fn [res tx-data] (println res))
            :on-error (fn [err tx-data] (println (str "Error: " err)))}})))))

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
  (edn-xhr
    {:method :get
     :url "/init"
     :on-complete
     (fn [res]
       (reset! app-state res)
       (om/root app-view app-state
                {:target (gdom/getElement "app")
                 :shared {:tx-chan tx-pub-chan}
                 :tx-listen (fn [tx-data root-cursor]
                              (put! tx-chan [tx-data root-cursor]))}))}))
