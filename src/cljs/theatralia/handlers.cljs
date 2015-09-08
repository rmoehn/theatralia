(ns theatralia.handlers
  "Event handlers for the Theatralia client.

  In the following I will write as if the handlers mutated the database, where
  actually, they only return transaction data. I.e. data that specifies how to
  mutate the database."
  (:require-macros [plumbing.core :refer [<- fn->]])
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [datascript :as d]
            [medley.core :as medley]
            [plumbing.core :as plumbing :refer [safe-get]]
            [re-frame.core :as rf]
            re-frame.handlers
            [re-frame.middleware :as middleware]
            [re-frame.utils :as rf-utils]
            [theatralia.db :as db]
            [theatralia.queries :as queries]
            [theatralia.thomsky :as tsky]
            [theatralia.utils :as th-utils :include-macros true]))

;;;; Various helpers

(defn pull-single
  "Same as (safe-get (pull db [attr] eid) attr), i. e. it saves you from typing
  the extra map lookup and, especially, mistyping the second occurence of ATTR."
  [db attr eid]
  (safe-get (d/pull db [attr] eid) attr))


;;;; Handlers for events around key-value areas

(defn kv-area-new-eh
  "Install key-value area identified by HANDLE."
  [_ [handle]]
  [{:db/id -1
    :kv-area/handle handle}])
(tsky/register-handler :kv-area/new kv-area-new-eh)

;; TODO: Can we use :db.fn/cas? (RM 2015-09-03)
(defn kv-area-set-eh
  "Set V as the value of key K in the key-value area indentified by HANDLE."
  [db [kv-handle k v]]
  {:pre [kv-handle v (keyword? k)]}
  (let [kv-cell-eid (d/q '[:find ?c .
                           :in $ ?h ?k
                           :where [?a :kv-area/handle ?h]
                                  [?a :kv-area/cells ?c]
                                  [?c :kv-cell/key ?k]]
                         db kv-handle k)]
    (if (some? kv-cell-eid)
      [{:db/id kv-cell-eid
        :kv-cell/val v}]
      [{:db/id -1
        :kv-cell/key k
        :kv-cell/val v}
       {:db/id [:kv-area/handle kv-handle]
        :kv-area/cells -1}])))
(tsky/register-handler :kv-area/set kv-area-set-eh)


;;;; Miscellaneous handlers

(re-frame.handlers/register-base :initialize tsky/set-up-datascript!)

(defn request-errored-eh
  "Handles the event that an XHR request errored.

  Reports the error of the XHRs. Leaves database unchanged."
  [_ [url error-map]]
  (rf-utils/error "Request to URL " url " errored: " error-map)
  [])
(tsky/register-handler :request/errored request-errored-eh)


;;;; Handlers for events around the material search

;; FIXME: Server errors when the search string starts with */%2a. (RM
;;        2015-07-09)
;; TODO: Use retract-entities. (RM 2015-09-03)
(defn search-submitted-eh
  "Handles the event that a search string was submitted.

  The search string should be at the key :searchInput in the key-value area
  identified by KV-HANDLE.

  Sends XHR, searching for materials. Clears results of current search."
  [db [kv-handle]]
  (let [search-string (safe-get (db/kv-area-as-map db kv-handle) :searchInput)
        url (str "/gq/" (th-utils/url-encode search-string))
        cur-search-result-eid (d/q '[:find ?e .
                                     :where [?e :search/result _]]
                                   db)]
    (when search-string
      (ajax/GET url
                {:format :edn
                 :handler #(rf/dispatch [:search/returned %])
                 :error-handler #(rf/dispatch [:request/errored url %])}))
    (if cur-search-result-eid
      [[:db.fn/retractEntity cur-search-result-eid]]
      [])))
(tsky/register-handler :search/submitted search-submitted-eh)

;; TODO: Define a format somewhere. (RM 2015-07-02)
(defn search-returned-eh
  "Handles the event that the server returned search results.

  SEARCH-RESULT should be a sequence of [eid material-title score] tuples.

  Transacts received search result into the database."
  [db [search-result]]
  (let [entid (d/q '[:find ?e . :where [?e :search/result _]] db)]
    [{:db/id (or entid -1)
      :search/result (set search-result)}]))
(tsky/register-handler :search/returned search-returned-eh)


;;;; Handlers for events around the tags list of the material input form

(defn tags-add-empty-eh
  "Puts a new empty tag at INDEX into the DB."
  [db [index]]
  [{:db/id -1
    :tag/s-id index
    :tag/text ""}])
(tsky/register-handler :tags/add-empty tags-add-empty-eh)

(defn tags-set-eh
  "Sets the contents of the tag identified by INDEX to TEXT."
  [db [index text]]
  [{:db/id [:tag/s-id index]
    :tag/text tag}])
(tsky/register-handler :tags/set tags-set-eh)

(defn tags-remove-eh
  "Removes the tag at INDEX from the DB."
  [db [index]]
  [[:db.fn/retractEntity [:tag/s-id index]]])
(tsky/register-handler :tags/remove tags-remove-eh)


;;;; Handlers for events around the material input form

;; REFACTOR: Make this a bit more self-documenting. (RM 2015-09-08)
(defn add-material-submit-eh
  "Handles the event that data for a new material were submitted.

  Expects the data to be in the key-value area identified by KV-HANDLE and in
  the entities in the database. The key-value area can have the following
  entries: :newMatTitle, :newMatURI, :newMatComments.

  Sends the data to the server. Leaves database unchanged."
  [db [kv-handle]]
  (let [tags (->> (d/q queries/tags-query db)
                  (map second)
                  (remove empty?)
                  distinct)
        mat-data (->> (db/kv-area-as-map db kv-handle)
                      (plumbing/map-keys (fn->
                                           name
                                           (string/replace #"^newMat" "")
                                           string/lower-case
                                           keyword))
                      (<- (select-keys #{:title :uri :comments})
                          (assoc :tags tags))
                      (medley/remove-vals empty?))]
    (ajax/POST "/materials"
               {:format :edn
                :params mat-data
                :handler #(rf/dispatch [:add-material/success kv-handle])
                :error-handler #(rf/dispatch [:request/errored "/materials"
                                              mat-data %])}))
  [])
(tsky/register-handler :add-material/submit add-material-submit-eh)

;; REFACTOR: Factor out retracting a key-value area. (RM 2015-09-08)
;; Note: The key-value area for the add-material-view is created when the view
;;       is created and the view holds on to the handle. Therefore we only clear
;;       the key-value area, but don't retract the handle.
(defn add-material-success-eh
  "Handles the event that the server has successfully stored a new material.

  Expects the same data to be present in DB as add-material-success-eh.

  Retracts all those data from the DB."
  [db [kv-handle]]
  [[:db.fn/call db/retract-entities
    '[:find [?c ...]
      :in $ ?h
      :where [?a :kv-area/handle ?h]
             [?a :kv-area/cells ?c]]
    kv-handle]
   [:db.fn/call db/retract-entities '[:find [?e ...] :where [?e :tag/s-id _]]]])
(tsky/register-handler :add-material/success add-material-success-eh)
