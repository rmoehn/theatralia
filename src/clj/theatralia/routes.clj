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
            [compojure.core :as cj]
            [compojure.route :as route]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [datomic.api :as d]
            [theatralia.welcome-page :as wp]
            [theatralia.database.canned-queries :as qcan]
            [theatralia.database.txd-gen :as txd-gen])
  (:import java.util.concurrent.ExecutionException))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

;;;; Utilities for request handling

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

;;;; Some handlers.

(defn search-for [conn s]
  (generate-response
    (sort-by #(nth % 2) >
             (d/q '[:find ?e ?t ?sc
                    :in $ ?s
                    :where [(fulltext $ :material/title ?s) [[?e ?t _ ?sc]]]]
                  (d/db conn) s))))

(defn save-material
  "Add material described by m to the database. Since currently multi-user
  capabilities are note implemented, the owner will be sandbox." ; ->LIVE-SPEC
  [conn m]
  (let [db (d/db conn)
        sandbox-eid (qcan/get-sandbox-eid db)
        [tag-eids tags-txd] (txd-gen/add-tags-txd conn (m :tags) sandbox-eid)
        mat-txd (txd-gen/add-material-txd m tag-eids sandbox-eid)]
    (try
      @(d/transact conn (conj tags-txd mat-txd))
      (catch ExecutionException e
        (do
          (-> e .getCause .printStackTrace)
          (generate-response "Transaction failed" 500))))
    (generate-response :ok))) ; ->LIVE-SPEC

;;;; The actual component

(defn make-handler [routes]
  (-> (let [conn (:conn (:database routes))]
        (cj/routes
          ;; Old server-side stuff
          (cj/GET "/" [] (wp/index))
          (cj/GET "/main.css" [] (wp/main-css))

          ;; REST (?) interface for client-side application
          (cj/GET "/gq/:s" [s] (search-for conn s))
          (cj/POST "/materials" {new-material :edn-params}
                                (save-material conn new-material))

          ;; Fallback handlers
          (route/resources "/")
          (route/not-found "<h1>Page not found</h1>")))
      wrap-edn-params))

(defrecord Routes [database])

(defn make-routes []
  (component/using (map->Routes {})
                   [:database]))
