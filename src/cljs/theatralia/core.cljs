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

;;;; Some utilities

(enable-console-print!)

(defn print-error [{:keys [error]}]
  (println "Error:" error))

(defn handle-change [event kw owner]
  (om/set-state! owner kw (.. event -target -value)))

;;;; App state and its API

(def app-state
  (atom {:search-result []}))

(defn search-result []
  (om/ref-cursor (:search-result (om/root-cursor app-state))))

;;;; Input field part of search

(defn process-input [owner]
  (let [input (.-value (om/get-node owner "search-input"))]
    (when input
      (edn-xhr {:method :get
                :url (str "/gq/" input)
                :on-complete (fn [res]
                               (om/transact! (search-result) #(vec res)))
                :on-error print-error}))))

(defn search-view [_ owner]
  (reify
    om/IInitState
    (init-state [_] {:text ""})

    om/IRenderState
    (render-state [this local-state]
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput] (kioo/set-attr
                           :value (:text local-state)
                           :onChange #(handle-change % :text owner)
                           :onKeyDown #(when (= (.-key %) "Enter")
                                         (process-input owner)))
         [:#submit] (kioo/set-attr
                      :onClick #(process-input owner))}))))

;;;; Result part of search

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

;;;; Adding new materials

(defn process-new-material [form-state owner]
  (edn-xhr {:method :post
            :url "/materials"
            :data form-state
            :on-complete #(om/update-state! owner (constantly {}))
            :on-error print-error}))

(defn bind-to [form-state owner]
  (fn [node]
    (let [kw (keyword (get-in node [:attrs :ref]))]
      ((kioo/set-attr :value (get form-state kw "")
                      :onChange #(handle-change % kw owner))
       node))))

(defn add-material-view [_ owner]
  (reify
    om/IInitState
    (init-state [this] {})

    om/IRenderState
    (render-state [this form-state]
      (kioo/component "templates/sandbox.html" [:#add-material-form]
        {[:#newMatSubmit] (kioo/set-attr
                            :onClick (fn [e]
                                       (.preventDefault e)
                                       (process-new-material form-state owner)))
         [:.form-control] (bind-to form-state owner)}))))

;;;; Wiring everything together

(defn sandbox-view [_ owner]
  (reify
    om/IRender
    (render [_]
      (kioo/component "templates/sandbox.html"
        {[:#search-field]
         (kioo/substitute (om/build search-view nil))

         [:#search-results]
         (kioo/substitute (om/build result-view nil))

         [:#add-material-form]
         (kioo/substitute (om/build add-material-view nil))}))))

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
