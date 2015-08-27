(ns theatralia.handlers
  (:require [ajax.core :as ajax]
            [datascript :as d]
            [plumbing.core :as plumbing :refer [safe-get]]
            [re-frame.core :as rf]
            [theatralia.utils :as th-utils]))

;;;; Various helpers

(defn pull-single
  "Same as (safe-get (pull db [attr] eid) attr), i. e. it saves you from typing
  the extra map lookup and, especially, mistyping the second occurence of ATTR."
  [db attr eid]
  (safe-get (d/pull db [attr] eid) attr))


;;;; All the handlers

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
