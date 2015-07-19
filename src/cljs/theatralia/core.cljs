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

(defn pull-single
  "Same as (safe-get (pull db [attr] eid) attr), i. e. it saves you from typing
  the extra map lookup and, especially, mistyping the second occurence of ATTR."
  [db attr eid]
  (safe-get (d/pull db [attr] eid) attr))


;;;; Event handlers

;; FIXME: Server errors when the search string starts with */%2a. (RM
;;        2015-07-09)
(defn search-submitted
  "Send XHR searching for materials. Clear results of current search."
  [db [scratch-entid]]
  (let [search-string (pull-single db :searchInput scratch-entid)
        url (str "/gq/" (th-utils/url-encode search-string))
        cur-search-result-eid (d/q '[:find ?e .
                                     :where [?e :search-result _]]
                                   db)]
    (when search-string
      (ajax/GET url
                {:format :edn
                 :handler #(rf/dispatch [:search-returned %])
                 :error-handler #(rf/dispatch [:request-errored url %])}))
    (if cur-search-result-eid
      [[:db.fn/retractEntity cur-search-result-eid]]
      [])))
(th-utils/register-handler* search-submitted)

;; TODO: Define a format somewhere. (RM 2015-07-02)
(defn search-returned
  "Transact received search result into the database."
  [db [search-result]]
  (let [entid (d/q '[:find ?e . :where [?e :search-result _]] db)]
    [{:db/id (or entid -1)
      :search-result (set search-result)}]))
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
    :scratch/key scratch-key}])
(th-utils/register-handler* new-scratch)

(defn set-scratch-val
  "Set V as the value of key K in the scratch area indentified by
  SCRATCH-ENTID."
  [db [scratch-entid k v]]
  {:pre [scratch-entid (keyword? k)]}
  [{:db/id scratch-entid
    k v}])
(th-utils/register-handler* set-scratch-val)


;;;; Subscription handlers

(defn get-scratch-entid
  "Entity ID of the scratch area with SCRATCH-KEY as the value of its
  :scratch/key attribute. Assumes that this scratch area already exists."
  [conn [_ scratch-key]]
  (let [r (tsky/bind '[:find ?e .
                       :in $ ?key
                       :where [?e :scratch/key ?key]]
                     conn scratch-key)]
    (assert @r)
    r))
(th-utils/register-sub* get-scratch-entid)

(defn get-scratch-contents
  "Contents of scratch area with entity ID SCRATCH-ENTID."
  [conn [_ scratch-entid]]
  (reaction (d/pull @conn '[*] scratch-entid)))
(th-utils/register-sub* get-scratch-contents)

(defn search-result
  "Result of the material search."
  [db []]
  (tsky/bind '[:find [?rs ...]
               :where [_ :search-result ?rs]]
             db))
(th-utils/register-sub* search-result)


;;;; Fns for dealing with the scratch part of the database

;; Instead of a random SQUUID we could also take an argument to use as prefix
;; and then a random number. Or just the provided scratch-key, but we'd have to
;; do check for collisions, which could be ugly.
(defn get-scratch
  "Adds a scratch entity to the app-db and returns a pair [eid ratom].

  eid is the ID of the scratch entity. It has at least one attribute,
  :scratch/key, a unique key which identifies it. Being a scratch space, you can
  attach arbitrary other attributes to it. If you get problems with attribute
  values that are collections, have a look at
  https://github.com/tonsky/datascript/issues/69 and at the mayhem around commit
  078546e9.

  ratom is a reactive atom holding all the attributes of the scratch entity."
  []
  (let [scratch-key (uuid/make-random-squuid)]
    (rf/dispatch-sync [:new-scratch scratch-key])
    (let [scratch-entid @(rf/subscribe [:get-scratch-entid scratch-key])

          scratch-contents-ra
          (rf/subscribe [:get-scratch-contents scratch-entid])]
      [scratch-entid scratch-contents-ra])))

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
    (let [id (keyword (plumbing/safe-get-in node [:attrs :id]))
          default-attrs
          [:value (get @scratch-ratom id "")
           :onChange #(rf/dispatch [:set-scratch-val scratch-id id (value %)])]]
      ((apply kioo/set-attr
              (concat default-attrs attrs)) node))))


;;;; Views

;;; For the search view

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

;;; View for adding materials

(defn add-material-view []
  (let [scratch (get-scratch)]
    (fn add-material-view-infn []
      (kioo/component "templates/sandbox.html" [:#add-material-form]
        {[:#newMatSubmit]
         (kioo/set-attr
           :onClick (fn [e]
                      (.preventDefault e)
                      (dispatch-scratch [:add-material scratch])))
         [:.form-control] (bind-and-set-attr scratch)}))))

;;; Root view

(defn root-view
  "The application's high-level structure."
  []
  (kioo/component "templates/sandbox.html"
    {[:#search-field] (kioo/substitute [search-view])
     [:#search-results] (kioo/substitute [result-view])
     [:#add-material-form] (kioo/substitute [add-material-view])}))


;;;; Entry point

(def schema {:scratch/key {:db/unique :db.unique/identity}
             :search-result {:db/cardinality :db.cardinality/many}})

(rf/dispatch-sync [:initialize schema])
(reagent/render [root-view]
                (js/document.getElementById "app"))

(comment

  (in-ns 'theatralia.core)

  @re-frame.db/app-db

  (d/transact! re-frame.db/app-db
             [{:db/id 5
               :scratch/val "bla"
               (keyword "blu") "bli"}])

  (rf/dispatch [:set-scratch-val 1 :bla "blu"])

  (def r (rf/subscribe [:get-scratch-val 1]))

  )
