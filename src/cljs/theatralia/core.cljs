(ns theatralia.core
  "All we have of the client right now.

  Defines an application with a part for searching and a part for adding
  materials. Installs it in the HTML element with ID \"app\" on the page where
  it is loaded."
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [reagent.core :as reagent]
            [thomsky.core :as th]
            [thomsky.subs :as subs]
            [thomsky.datascript :as thsd]
            [datascript :as d]
            [kioo.reagent :as kioo :include-macros true]
            kioo.util ; so that kioo/component won't cause warnings
            [kioo.core :as kioo-core]))
 ; Deleted all macro stuff. We might need it.

;;; Credits:
;;;  - https://github.com/ckirkendall/kioo
;;;  - https://github.com/ckirkendall/todomvc/blob/gh-pages/labs/architecture-examples/kioo/src/todomvc/app.cljs
;;;  - https://gist.github.com/allgress/11348685

(enable-console-print!)

(defn name-query [db [_]]
  (reaction @(thsd/bind db '[:find ?t . :where [?e :text ?t]])))

(def app (-> (th/make-thomsky-app)
             (subs/register-sub :name-query name-query)))

(defn greet []
  (let [name-ratom (subs/subscribe app [:name-query])]
    (fn [] [:div "Neato, " @name-ratom])))

(comment
(def result (thsd/bind app-db '[:find ?t :where [?e :text ?t]]))
(def a-name (reaction (ffirst @result)))
(def letters (reaction (count @a-name)))

(thsd/bind (:app-db app) '[:find ?t . :where [?e :text ?t]])

  (in-ns 'theatralia.core)

  app

  ((greet))

  (:app-db app)

  (d/transact! (:app-db app) [{:db/id 1 :text "Gory"}])

  @(name-query (:app-db app) [:x])

  (def the-name (name-query (:app-db app) [:x]))

  @the-name

  ((greet))

(subs/subscribe app [:name-query])

  )

(comment

  (defn search-string [db [_]]
  (reaction @(bind @db '[:find ?t . :where [?e :scratch/search-string ?t]])))
(rf/register-sub :search-string search-string)

(defn search-view []
  (let [search-string (rf/subscribe [:search-string])]
    (fn []
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput] (kioo/set-attr
                           :value @search-string
                           :on-click #(rf/dispatch [:chg-search-field %]))}))))

(reagent/render [search-view] (js/document.getElementById "app")))

(comment

  (in-ns 'theatralia.core)

@result
@a-name

@ratom2

(greet)


(def greeting (reaction (greet a-name)))

@a-name

(d/transact! app-db [{:db/id -11 :scratch/search-string "Mario"}])

@greeting

(do
  @greeting))
