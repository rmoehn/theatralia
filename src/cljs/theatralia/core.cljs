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

(enable-console-print!)

(handlers/register-base :initialize tsky/set-up-datascript!)

(defn partitionv [& args]
  (map vec (apply partition args)))

(defn value [text-change-event]
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

(defn search-submitted [db [scratch-key]]
  (let [search-string
        ,,(get (d/q '[:find ?m .
                      :in $ ?sk
                      :where [?e :scratch/key ?sk]
                      [?e :scratch/val ?m]]
                    db scratch-key) "searchInput")
        url (str "/gq/" (th-utils/url-encode search-string))]
    (when search-string
      (ajax/GET url
                {:format :edn
                 :handler #(rf/dispatch [:search-returned %])
                 :error-handler #(rf/dispatch [:request-errored url %])})))
  [])

(th-utils/register-handler* search-submitted)

;; TODO: Define a format somewhere. (RM 2015-07-02)
(defn search-returned [db [search-result]]
  (let [entid (d/q '[:find ?e . :where [?e :search-result _]] db)]
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

;; What do we want to do?
;;  - Bind a text field to the database.
;;     - Updates to the text field should be transacted to the database.
;;     - Value of the text field comes from the database.
;;  - A request handler should be able to find the value in the database.
;;  - Want to use a single name for all that, the ID of the text field.
;;  - For the Kioo substitution stuff, we should also be able to only use a
;;    generic class.
;;
;; What is the problem?
;;  - In Om we just provided the local state, which we could write to and read
;;    from. We bound the node to the local state and the node could write and
;;    read at an arbitrary key.
;;  - Here we have the problem that we need a Reaction…
;;  - We have to create the Reaction outside and already provide a name.
;;    However, nobody said that the name has to be the ID of the text field. It
;;    could just be some "area" in the app-db where the text field can put its
;;    data under an arbitrary key.
;;  - Such stuff should already exist. Everybody needs it. re-com? Doesn't work
;;    with Datascript, I guess. Oh, it does. It's just the view.
;;  - But we don't need

(defn get-scratch-val [conn [_ scratch-key]]
  (reaction (or @(tsky/bind '[:find ?m .
                              :in $ ?key
                              :where [?e :scratch/key ?key]
                                     [?e :scratch/val ?m]]
                            conn scratch-key)
                {})))
(th-utils/register-sub* get-scratch-val)

(defn set-scratch-val [db [scratch-key k v]]
  {:pre [scratch-key k v]}
  (let [[entid m]
        (d/q '[:find [?e ?m]
                      :in $ ?sk
                      :where [?e :scratch/key ?sk]
                             [?e :scratch/val ?m]]
                    db scratch-key)]
    [{:db/id (or entid -1)
      :scratch/key scratch-key
      :scratch/val (assoc (or m {}) k v)}]))
(th-utils/register-handler* set-scratch-val)

;; Instead of a completely random UUID we could also take an argument to use as
;; prefix and then a random number. Or just the provided scratch-key, but then
;; we'd have to do check for collisions, which could be ugly.
(defn get-scratch []
  (let [scratch-key (uuid/make-random-uuid)]
    [scratch-key (rf/subscribe [:get-scratch-val scratch-key])]))

(defn bind-and-set-attr [[scratch-key scratch-ratom] & attrs]
  {:pre [(even? (count attrs))]}
  (fn [node]
    (let [id (plumbing/safe-get-in node [:attrs :id])
          default-attrs
          [:value (get @scratch-ratom id "")
           :onChange #(rf/dispatch [:set-scratch-val scratch-key id (value %)])]]
      ((apply kioo/set-attr
              (concat default-attrs attrs)) node))))

(defn dispatch-scratch [[request-id [scratch-id _] & other]]
  (rf/dispatch (into [request-id scratch-id] other)))

(defn search-result [db []]
  (reaction @(tsky/bind '[:find ?rs .
                          :where [_ :search-result ?rs]]
                        db)))
(th-utils/register-sub* search-result)

(defn result-item [[id title _]]
  (kioo/component "templates/sandbox.html"
    [:#search-results :> :ol :> first-child]
    {[:li] (kioo/do-> (kioo/content title)
                      (kioo/set-attr :key id))}))

(defn result-view []
  (let [results-ra (rf/subscribe [:search-result])]
    (kioo/component "templates/sandbox.html" [:#search-results]
      {[:ol] (kioo/content (map result-item @results-ra))})))

(defn search-view []
  (let [scratch (get-scratch)]
    (fn []
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

(defn root-view []
  (kioo/component "templates/sandbox.html"
    {[:#search-field] (kioo/substitute [search-view])
     [:#search-results] (kioo/substitute [result-view])}))

(rf/dispatch-sync [:initialize])
(reagent/render [root-view]
                (js/document.getElementById "app"))



(comment

  (in-ns 'theatralia.core)

  (rf/dispatch [:set-current-text "search-field" "huhu"])

  @re-frame.db/app-db

  (d/q '[:find ?t :where [?e :search-result ?t]] @re-frame.db/app-db)

  @(get-current-text db/app-db [:bla "search-field"])

  @(def ra (rf/subscribe [:get-scratch-val :search-view]))

  (get-scratch :search-view)

  (def res *1)

  @(res 1)

  ((bind-and-set-attr (get-scratch :search-view)) {:attrs {:id "searchInput"}} :onKeyDown :bla)

  (bind-and-set-attr
    (get-scratch :search-view)
    :onKeyDown
    #(when (= (.-key %) "Enter")
       (rf/dispatch [:search-submitted :search-view])))

(d/q '[:find [?e ?m]
                      :in $ ?sk
                      :where [?e :scratch/key ?sk]
                             [?e :scratch/val ?m]]
                    @re-frame.db/app-db #uuid "b90a7201-53cd-44eb-9257-66198cd6e616")

  )
