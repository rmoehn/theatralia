(ns theatralia.core
  "All we have of the client right now.

  Defines an application with a part for searching and a part for adding
  materials. Installs it in the HTML element with ID \"app\" on the page where
  it is loaded."
  (:require [devtools.core :as devtools]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [theatralia.db :as db]
            theatralia.handlers
            theatralia.subs
            [theatralia.views :as views]))

;;; Credits:
;;;  - https://github.com/ckirkendall/kioo
;;;  - https://github.com/ckirkendall/todomvc/blob/gh-pages/labs/architecture-examples/kioo/src/todomvc/app.cljs

(enable-console-print!)

(devtools/install!)

(rf/dispatch-sync [:initialize db/schema])
(reagent/render [views/root-view]
                (js/document.getElementById "app"))
