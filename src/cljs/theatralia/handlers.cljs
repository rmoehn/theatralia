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

;; TODO: Rename index to s-id (serial ID) or something similar. (RM 2015-08-28)
(defn new-tag
  [db [index]]
  [{:db/id -1
    :tag/index index
    :tag/content ""}])
(th-utils/register-handler* new-tag [middleware/debug])

(defn remove-tag
  [db [index]]
  [[:db.fn/retractEntity [:tag/index index]]])
(th-utils/register-handler* remove-tag [middleware/debug])

(defn tag-change
  [db [index tag]]
  [{:db/id [:tag/index index]
    :tag/content tag}])
(th-utils/register-handler* tag-change [middleware/debug])

(defn add-material
  [db [scratch-entid]]
  (let [tags (->> (d/q queries/tags-query db)
                  (map second)
                  (remove empty?)
                  distinct)
        mat-data (->> (d/pull db '[*] scratch-entid)
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
                :handler #(rf/dispatch [:material-added scratch-entid])
                :error-handler #(rf/dispatch [:request-errored "/materials"
                                              mat-data %])}))
  [])
(th-utils/register-handler* add-material)

(defn retract-entities [db q]
  (let [eids (d/q q db)]
    (map (fn [eid] [:db.fn/retractEntity eid]) eids)))

(defn material-added
  [db [scratch-entid]]
  [[:db.fn/retractEntity scratch-entid]
   [:db.fn/call retract-entities '[:find ?e :where [?e :tag/index _]]]])
(th-utils/register-handler* material-added)
