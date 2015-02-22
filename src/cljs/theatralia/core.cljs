(ns theatralia.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.util :refer [edn-xhr]]
            [kioo.om :as kiom]
            kioo.util ; so that kioo/component won't cause warnings
            [kioo.core :as kioo :include-macros true]))

;;; Credits:
;;;  - https://github.com/swannodette/om
;;;  - https://github.com/swannodette/om/wiki/Basic-Tutorial
;;;  - https://github.com/omcljs/om/wiki/Advanced-Tutorial
;;;  - https://github.com/ckirkendall/kioo
;;;  - https://github.com/ckirkendall/todomvc/blob/gh-pages/labs/architecture-examples/kioo/src/todomvc/app.cljs

(enable-console-print!)

(def app-state
  (atom {:search-result []}))

(defn search-result []
  (om/ref-cursor (:search-result (om/root-cursor app-state))))

(defn handle-change [event owner {:keys [text]}]
  (om/set-state! owner :text (.. event -target -value)))

(defn process-input [owner]
  (let [input (.-value (om/get-node owner "search-input"))]
    (when input
      (edn-xhr {:method :get
                :url (str "/gq/" input)
                :on-complete (fn [res]
                               (om/transact! (search-result) #(vec res)))
                :on-error (fn [{:keys [error]}] (println "Error:" error))}))))

(defn result-item [[id title score] owner]
  (om/component
    (kioo/component "templates/sandbox.html"
      [:#search-results :> :ol :> first-child]
      {[:li] (kioo/content title)})))

(defn result-view [_ owner]
  (reify
    om/IRender
    (render [this]
      (let [rs (om/observe owner (search-result))]
        (kioo/component "templates/sandbox.html" [:#search-results]
          {[:ol] (kioo/content (om/build-all result-item rs))})))))

(defn search-view [_ owner]
  (reify
    om/IInitState
    (init-state [_] {:text ""})

    om/IRenderState
    (render-state [this local-state]
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput] (kioo/set-attr
                           :value (:text local-state)
                           :onChange #(handle-change % owner local-state)
                           :onKeyDown #(when (= (.-key %) "Enter")
                                         (process-input owner)))
         [:#submit] (kioo/set-attr
                      :onClick #(process-input owner))}))

    om/IDidMount
    (did-mount [_]
      (.focus (om/get-node owner "search-input")))))

(defn sandbox-view [_ owner]
  (reify
    om/IRender
    (render [_]
      (kioo/component "templates/sandbox.html"
        {[:#search-field]
         (kioo/substitute (om/build search-view nil))

         [:#search-results]
         (kioo/substitute (om/build result-view nil))}))))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build sandbox-view nil))))

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
  (om/root app-view app-state
           {:target (gdom/getElement "app")
            :shared {:tx-chan tx-pub-chan}
            :tx-listen (fn [tx-data root-cursor]
                         (put! tx-chan [tx-data root-cursor]))}))
