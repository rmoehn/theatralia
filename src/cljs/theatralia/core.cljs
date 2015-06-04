(ns theatralia.core
  "All we have of the client right now.

  Defines an application with a part for searching and a part for adding
  materials. Installs it in the HTML element with ID \"app\" on the page where
  it is loaded."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [clojure.string :as string]
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

(def events (chan))

(defn search-result []
  (om/ref-cursor (:search-result (om/root-cursor app-state))))

(defn rc-top-result []
  (let [res (first (search-result))]
    (println (type res))
    res))

(defn rc-processed-top-result []
  (when-let [[i t s] (rc-top-result)]
    [i (string/upper-case t) s]))

(defn maybe-observe [owner maybe-ref-cursor]
  (when maybe-ref-cursor (om/observe owner maybe-ref-cursor)))

(defn top-result []
  (first (to-observe)))

; - Want to create a signal from a signal.
; - In this case a (search-result) is a signal. I can observe it.
; - It should be possible to create a signal B from signal A without observing
;   signal A. Only want to observe signal B.
; - What is the problem? In search-result om/ref-cursor always gets a cursor to
;   a vector.
; - If I want to make a signal from this that wants something out of this
;   vector, the something has to be a data structure itself in order to still be
;   a signal. We can't get out primitives, because they can't be signalic.
; - That's the general problem. Primitives can't be signals. This introduces two
;   issues:
;     - Whenever we want a primitive to be a signal, we have to wrap it before
;       putting it into the app-state and unwrap it after taking it out. This
;       makes the code ugly. It could be mitigated some Lispy cunning, but I
;       have a feeling that there are quite a lot of places that would need
;       fixing. Not justifiable.
;     - Even if we have collections of non-primitives, these collections can be
;       empty and operations applied to them (like get) now return a primitive:
;       nil. Having to wrap nil would require to mitigate even more things,
;       since nil is used as a special value in a number of places and [nil]
;       can't work there.
; - How often do we need primitives that are signals?
;     - Everything that is some sort of a label would be a primitive signal. For
;       example, the value of an input field (locals made global) is a primitive
;       signal.
;     - See the second subitem of the previous item.
; - All in all a pretty sad diagnosis. Can this be fixed? Can Om have signals of
;   everything? Hmm, as long as we pass the owner all the way down, it's fine.

(defn processed-top-result [owner]
  (when-let [[i t s] (top-result owner)]
    [i (string/upper-case t) s]))

(defn processed-search-result [owner]
  (map (fn [[i t s]] [i (string/upper-case t) s])
       (om/observe owner (search-result))))

;;;; Input field part of search

(defn process-search [owner]
  (when-let [input (.-value (om/get-node owner "search-input"))]
    (put! events [:perform-search
                  (.-value (om/get-node owner "search-input"))])))

(defn search-view [_ owner]
  (reify
    om/IInitState
    (init-state [_] {:text ""})

    om/IRenderState
    (render-state [this local-state]
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput]
         (kioo/set-attr
           :value (:text local-state)
           :onChange #(handle-change % :text owner)
           :onKeyDown #(when (= (.-key %) "Enter") (process-search owner)))
         [:#submit] (kioo/set-attr :onClick #(process-search owner))}))))

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

(defmulti route (fn [app-state [ev-key & data]] ev-key))

(defmethod route :perform-search [app-state [_ s]]
  (edn-xhr {:method :get
            :url (str "/gq/" s)
            :on-complete (fn [res]
                           (put! events [:got-search-results (vec res)]))
            :on-error print-error})
  app-state)

(defmethod route :got-search-results [app-state [_ r]]
  (println app-state)
  (assoc app-state :search-result r))

(defmethod route :default [app-state x]
  (println "Got request" x)
  app-state)

(defn handle [event]
  (println app-state (om/root-cursor app-state))
  (om/transact! (om/root-cursor app-state) #(route % event))
  (println app-state (om/root-cursor app-state)))

(go (while true (handle (<! events))))

(om/root app-view app-state
         {:target (gdom/getElement "app")})
