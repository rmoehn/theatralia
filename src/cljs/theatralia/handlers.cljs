(ns theatralia.handlers
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


;;;; All the handlers

(re-frame.handlers/register-base :initialize tsky/set-up-datascript!)

;; FIXME: Server errors when the search string starts with */%2a. (RM
;;        2015-07-09)
;; TODO: Use retract-entities. (RM 2015-09-03)
(defn search-submitted-eh
  "Send XHR searching for materials. Clear results of current search."
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
  "Transact received search result into the database."
  [db [search-result]]
  (let [entid (d/q '[:find ?e . :where [?e :search/result _]] db)]
    [{:db/id (or entid -1)
      :search/result (set search-result)}]))
(tsky/register-handler :search/returned search-returned-eh)

(defn request-errored-eh
  "Report errors of XHRs. Leave database unchanged."
  [_ [url error-map]]
  (rf-utils/error "Request to URL " url " errored: " error-map)
  [])
(tsky/register-handler :request/errored request-errored-eh)

(defn kv-area-new-eh
  "Install key-value area identified by HANDLE."
  [_ [handle]]
  [{:db/id -1
    :kv-area/handle handle}])
(tsky/register-handler :kv-area/new [middleware/debug] kv-area-new-eh)

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
(tsky/register-handler :kv-area/set [middleware/debug] kv-area-set-eh)

(defn tags-add-empty-eh
  [db [index]]
  [{:db/id -1
    :tag/s-id index
    :tag/text ""}])
(tsky/register-handler :tags/add-empty [middleware/debug] tags-add-empty-eh)

(defn tags-remove-eh
  [db [index]]
  [[:db.fn/retractEntity [:tag/s-id index]]])
(tsky/register-handler :tags/remove [middleware/debug] tags-remove-eh)

(defn tags-set-eh
  [db [index tag]]
  [{:db/id [:tag/s-id index]
    :tag/text tag}])
(tsky/register-handler :tags/set [middleware/debug] tags-set-eh)

;; REFACTOR: Make this a bit more self-documenting. (RM 2015-09-08)
(defn add-material-submit-eh
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

(defn retract-entities [db q & q-args]
  (let [eids (apply d/q q db q-args)]
    (map (fn [eid] [:db.fn/retractEntity eid]) eids)))

;; REFACTOR: Factor out retracting a key-value area. (RM 2015-09-08)
;; Note: The key-value area for the add-material-view is created when the view
;;       is created and the view holds on to the handle. Therefore we only clear
;;       the key-value area, but don't retract the handle.
(defn add-material-success-eh
  [db [kv-handle]]
  [[:db.fn/call retract-entities
    '[:find [?c ...]
      :in $ ?h
      :where [?a :kv-area/handle ?h]
             [?a :kv-area/cells ?c]]
    kv-handle]
   [:db.fn/call retract-entities '[:find [?e ...] :where [?e :tag/s-id _]]]])
(tsky/register-handler :add-material/success [middleware/debug] add-material-success-eh)
