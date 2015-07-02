(ns theatralia.core
  "All we have of the client right now.

  Defines an application with a part for searching and a part for adding
  materials. Installs it in the HTML element with ID \"app\" on the page where
  it is loaded."
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-frame.handlers :as handlers]
            [re-frame.utils :as rf-utils]
            re-frame.db
            [theatralia.thomsky :as tsky]
            [theatralia.utils :as th-utils :include-macros true]
            [datascript :as d]
            [ajax.core :as ajax]
            [kioo.reagent :as kioo :include-macros true]
            kioo.util ; so that kioo/component won't cause warnings
            [kioo.core :as kioo-core]))
 ; Deleted all macro stuff. We might need it.

;;; Credits:
;;;  - https://github.com/ckirkendall/kioo
;;;  - https://github.com/ckirkendall/todomvc/blob/gh-pages/labs/architecture-examples/kioo/src/todomvc/app.cljs

(enable-console-print!)

(handlers/register-base :initialize tsky/set-up-datascript!)

(defn cv [text-change-event]
  (-> text-change-event .-target .-value))

(defn set-current-text [db [input-field value]]
  (let [entid (d/q '[:find ?e .
                     :in $ ?id
                     :where [?e :scratch/input-field ?id]]
                   db input-field)]
    [{:db/id (or entid -1)
      :scratch/input-field input-field
      :value value}]))

(th-utils/register-handler* set-current-text)

(defn search-submitted [db []]
  (let [search-string (d/q '[:find ?t .
                             :where [?e :scratch/input-field "search-field"]
                                    [?e :value ?t]]
                           db)
        url (str "/gq/" search-string)]
    (ajax/GET url
              {:format :edn
               :handler #(rf/dispatch [:search-returned %])
               :error-handler #(rf/dispatch [:request-errored url %])}))
  [])

(th-utils/register-handler* search-submitted)

;; TODO: Define a format somewhere. (RM 2015-07-02)
(defn search-returned [db [search-result]]
  (let [entid (d/q '[:find ?e . :where [?e :search-result _]])]
    [{:db/id (or entid -1)
      :search-result search-result}]))

(th-utils/register-handler* search-returned)

(defn request-errored [_ [url error-map]]
  (rf-utils/error "Request to URL " url " errored: " error-map)
  [])

(defn get-current-text [conn [_ input-field]]
  (reaction @(tsky/bind '[:find ?t .
                          :in $ ?id
                          :where [?e :scratch/input-field ?id]
                                 [?e :value ?t]]
                        conn
                        input-field)))

(th-utils/register-sub* get-current-text)

(defn search-view []
  (let [current-text-ra (rf/subscribe [:get-current-text "search-field"])]
    (fn []
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput] (kioo/set-attr
                           :value @current-text-ra
                           :onChange
                           #(rf/dispatch [:set-current-text "search-field" (cv %)])
                           :onKeyDown
                           #(when (= (.-key %) "Enter")
                              (rf/dispatch [:search-submitted])))
         [:#submit] (kioo/set-attr :onClick #(rf/dispatch [:search-submitted]))}))))

(defn root-view []
  (kioo/component "templates/sandbox.html"
    {[:#search-field] (kioo/substitute [search-view])}))

(rf/dispatch-sync [:initialize])
(reagent/render [root-view]
                (js/document.getElementById "app"))







