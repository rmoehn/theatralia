(ns theatralia.core
  "All we have of the client right now.

  Defines an application with a part for searching and a part for adding
  materials. Installs it in the HTML element with ID \"app\" on the page where
  it is loaded."
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

;;;; Event handlers



;;;; Subscription handlers

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
