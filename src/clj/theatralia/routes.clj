(ns theatralia.routes
  "Defines the component for handling requests to the server. This component is
  constant, i. e. it cannot be started or stopped.

  Due to experimentation this is structured in a slightly strange way. The code
  for generating the server side HTML from the early stages of this project is
  sitting neatly in its own namespaces. The code for handling REST requests from
  the client application is lying around here.

  I expect everything that is not concerned with routing to be gradually
  extracted to other namespaces."
  (:require [com.stuartsierra.component :as component]
            [dire.core :refer [supervise with-handler]]
            [compojure.core :as cj]
            [compojure.route :as route]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [datomic.api :as d]
            [theatralia.welcome-page :as wp]
            [theatralia.database.canned-queries :as qcan]
            [theatralia.database.transactions :as tx]))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

;;;; Utilities for request handling

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

;;;; Some handlers.

(defn search-for [db s]
  (generate-response
    (sort-by #(nth % 2) >
             (d/q '[:find ?e ?t ?sc
                    :in $ ?s
                    :where [(fulltext $ :material/title ?s) [[?e ?t _ ?sc]]]]
                  db s))))

(defn add-material
  "Add material described by m to the database. Since currently multi-user
  capabilities are note implemented, the owner will be sandbox." ; ->LIVE-SPEC
  [conn m]
  (supervise #'tx/add-material conn m "sandbox")
  (generate-response :ok))

(with-handler #'add-material
  [:condition :could-not-add] ; Not the right namespace yet.
  #(generate-response "Couldn't add material." 500)) ; ->LIVE-SPEC

;;;; The actual component

(defn make-handler [routes]
  (-> (let [conn (:conn (:database routes))]
        (cj/routes
          ;; Old server-side stuff
          (cj/GET "/" [] (wp/index))
          (cj/GET "/main.css" [] (wp/main-css))

          ;; REST (?) interface for client-side application
          (cj/GET "/gq/:s" [s] (search-for (d/db conn) s))
          (cj/POST "/materials" {new-material :edn-params}
                                (supervise #'add-material conn new-material))

          ;; Fallback handlers
          (route/resources "/")
          (route/not-found "<h1>Page not found</h1>")))
      wrap-edn-params))

(defrecord Routes [database])

(defn make-routes []
  (component/using (map->Routes {})
                   [:database]))
