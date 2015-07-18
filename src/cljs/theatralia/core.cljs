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
            [cljs-uuid-utils.core :as uuid]
            [theatralia.thomsky :as tsky]
            [theatralia.utils :as th-utils :include-macros true]
            [datascript :as d]
            [ajax.core :as ajax]
            [plumbing.core :as plumbing :refer [safe-get]]
            [kioo.reagent :as kioo :include-macros true]
            kioo.util ; so that kioo/component won't cause warnings
            [kioo.core :as kioo-core]))
 ; Deleted all macro stuff. We might need it.

;;; Credits:
;;;  - https://github.com/ckirkendall/kioo
;;;  - https://github.com/ckirkendall/todomvc/blob/gh-pages/labs/architecture-examples/kioo/src/todomvc/app.cljs


;;;; Application setup

(enable-console-print!)

(handlers/register-base :initialize tsky/set-up-datascript!)


;;;; Various helpers

(defn partitionv
  "Like partition, but returns a sequence of vectors instead of a sequence of
  sequences."
  [& args]
  (map vec (apply partition args)))

(defn value
  "Returns the value of the input field whose change caused the
  TEXT-CHANGE-EVENT."
  [text-change-event]
  (-> text-change-event .-target .-value))


;;;; Event handlers

;; FIXME: Server errors when the search string starts with */%2a. (RM
;;        2015-07-09)
(defn search-submitted
  "Send XHR searching for materials."
  [db [scratch-entid]]
  (let [search-string (get-in (d/pull db [:scratch/val] scratch-entid)
                              [:scratch/val "searchInput"])
        url (str "/gq/" (th-utils/url-encode search-string))]
    (when search-string
      (ajax/GET url
                {:format :edn
                 :handler #(rf/dispatch [:search-returned %])
                 :error-handler #(rf/dispatch [:request-errored url %])})))
  [])
(th-utils/register-handler* search-submitted)

;; TODO: Define a format somewhere. (RM 2015-07-02)
(defn search-returned
  "Transact received search result into the database."
  [db [search-result]]
  (let [entid (d/q '[:find ?e . :where [?e :search-result _]] db)]
    [{:db/id (or entid -1)
      :search-result search-result}]))
(th-utils/register-handler* search-returned)

(defn request-errored
  "Report errors of XHRs. Leave database unchanged."
  [_ [url error-map]]
  (rf-utils/error "Request to URL " url " errored: " error-map)
  [])
(th-utils/register-handler* request-errored)

(defn new-scratch
  "Install scratch area with SCRATCH-KEY as its :scratch/key attribute's value."
  [_ [scratch-key]]
  [{:db/id -1
    :scratch/key scratch-key
    :scratch/val {}}])
(th-utils/register-handler* new-scratch)

(defn set-scratch-val
  "Set V as the value of key K in the scratch area indentified by
  SCRATCH-ENTID."
  [db [scratch-entid k v]]
  {:pre [scratch-entid k v]}
  (let [{m :scratch-val}
        (d/pull db [:scratch/val] scratch-entid)]
    [{:db/id scratch-entid
      :scratch/val (assoc (or m {}) k v)}]))
(th-utils/register-handler* set-scratch-val)


;;;; Subscription handlers

(defn get-scratch-entid
  "Entity ID of the scratch area with SCRATCH-KEY as the value of its
  :scratch/key attribute."
  [conn [_ scratch-key]]
  (tsky/bind '[:find ?e .
               :in $ ?key
               :where [?e :scratch/key ?key]]
             conn scratch-key))
(th-utils/register-sub* get-scratch-entid)

(defn get-scratch-val
  "Scratch area with entity ID SCRATCH-ENTID."
  [conn [_ scratch-entid]]
  (reaction
    (safe-get (d/pull @conn [:scratch/val] scratch-entid) :scratch/val)))
(th-utils/register-sub* get-scratch-val)

(defn search-result
  "Result of the material search."
  [db []]
  (tsky/bind '[:find ?rs .
               :where [_ :search-result ?rs]]
             db))
(th-utils/register-sub* search-result)


;;;; Fns for dealing with the scratch part of the database

;; Instead of a random SQUUID we could also take an argument to use as prefix
;; and then a random number. Or just the provided scratch-key, but we'd have to
;; do check for collisions, which could be ugly.
(defn get-scratch
  "Adds a scratch area to the app-db and returns a pair [eid ratom]. eid is the
  ID of an entity with two attributes: :scratch/key (a unique key identifying
  this scratch area) and :scratch/val (a map, the scratch area itself). ratom is
  a reactive atom holding the current value of :scratch/val."
  []
  (let [scratch-key (uuid/make-random-squuid)]
    (rf/dispatch-sync [:new-scratch scratch-key])
    (let [scratch-entid @(rf/subscribe [:get-scratch-entid scratch-key])
          scratch-val-ra (rf/subscribe [:get-scratch-val scratch-entid])]
      [scratch-entid scratch-val-ra])))

(defn dispatch-scratch
  "Convenience fn around rf/dispatch. Suppose s is what (get-scratch) returned.
  Instead of executing (rf/dispatch [some-request-id (first s) param1 …), you
  can pass the whole s like this: (dispatch-scratch [some-request-id s param1
  …)."
  [[request-id [scratch-entid _] & other]]
  (rf/dispatch (into [request-id scratch-entid] other)))


;;;; Fn for dealing with text input fields

(defn bind-and-set-attr
  "Binds a text input field to the given scratch space in the app-db and adds
  the ATTRS to its existing attributes."
  [[scratch-id scratch-ratom] & attrs]
  {:pre [(even? (count attrs))]}
  (fn [node]
    (let [id (plumbing/safe-get-in node [:attrs :id])
          default-attrs
          [:value (get @scratch-ratom id "")
           :onChange #(rf/dispatch [:set-scratch-val scratch-id id (value %)])]]
      ((apply kioo/set-attr
              (concat default-attrs attrs)) node))))


;;;; Views

(defn result-item
  "One item of the material search results."
  [[id title _]]
  (kioo/component "templates/sandbox.html"
    [:#search-results :> :ol :> first-child]
    {[:li] (kioo/do-> (kioo/content title)
                      (kioo/set-attr :key id))}))

(defn result-view
  "Material search results."
  []
  (let [results-ra (rf/subscribe [:search-result])]
    (fn []
      (kioo/component "templates/sandbox.html" [:#search-results]
        {[:ol] (kioo/content (map result-item @results-ra))}))))

(defn search-view
  "A group of components for searching materials."
  []
  (let [scratch (get-scratch)]
    (fn search-view-infn []
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput]
         (bind-and-set-attr
           scratch
           :onKeyDown
           #(when (= (.-key %) "Enter")
              (dispatch-scratch [:search-submitted scratch])))

         [:#submit]
         (kioo/set-attr :onClick #(dispatch-scratch [:search-submitted
                                                     scratch]))}))))

(defn root-view
  "The application's high-level structure."
  []
  (kioo/component "templates/sandbox.html"
    {[:#search-field] (kioo/substitute [search-view])
     [:#search-results] (kioo/substitute [result-view])}))


;;;; Entry point

(rf/dispatch-sync [:initialize])
(reagent/render [root-view]
                (js/document.getElementById "app"))
