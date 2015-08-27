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
            [plumbing.core :as plumbing :refer [safe-get]]))
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

(def tags-query '[:find ?index ?tag
                  :where [?e :tag/index ?index]
                         [?e :tag/content ?tag]])

(defn new-tag
  [db [index]]
  (let [existing-tags (d/q tags-query db)]
    [{:db/id -1
      :tag/index index
      :tag/content ""}]))
(th-utils/register-handler* new-tag)

(defn remove-tag
  [db [index]]
  [[:db.fn/retractEntity [:tag/index index]]])
(th-utils/register-handler* remove-tag)

(defn tag-change
  [db [index tag]]
  [{:db/id [:tag/index index]
    :tag/content tag}])
(th-utils/register-handler* tag-change)


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

(defn tags
  [db []]
  (let [query-res (tsky/bind tags-query db)]
    (reaction (sort-by first @query-res))))
(th-utils/register-sub* tags)


;;;; Views


;;;; Entry point

(def schema {:scratch/key {:db/unique :db.unique/identity}
             :search-result {:db/cardinality :db.cardinality/many}
             :tag/index {:db/unique :db.unique/identity}})

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
