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
            re-frame.db
            [theatralia.thomsky :as tsky]
            [theatralia.utils :as th-utils :include-macros true]
            [datascript :as d]
            [kioo.reagent :as kioo :include-macros true]
            kioo.util ; so that kioo/component won't cause warnings
            [kioo.core :as kioo-core]))
 ; Deleted all macro stuff. We might need it.

;;; Credits:
;;;  - https://github.com/ckirkendall/kioo
;;;  - https://github.com/ckirkendall/todomvc/blob/gh-pages/labs/architecture-examples/kioo/src/todomvc/app.cljs

(enable-console-print!)

(handlers/register-base :initialize tsky/set-up-datascript!)

(rf/dispatch-sync [:initialize])

(defn change-name [app-db [n]]
  [{:db/id 1 :text n}])

(th-utils/register-handler* change-name)

(defn name-query [db [_]]
  (reaction @(tsky/bind db '[:find ?t . :where [?e :text ?t]])))

(th-utils/register-sub* name-query)

(defn greet []
  (let [name-ratom (rf/subscribe [:name-query])]
    (fn [] [:div "Neato, " @name-ratom])))

(rf/dispatch [:change-name "Grocky"])

((greet))
