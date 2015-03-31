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
            [medley.core :refer [remove-vals]])
  (:import java.util.concurrent.ExecutionException))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

;;;; Utilities for request handling

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

;;;; Handler for searching materials

(defn search-for [conn s]
  (generate-response
    (sort-by #(nth % 2) >
             (d/q '[:find ?e ?t ?sc
                    :in $ ?s
                    :where [(fulltext $ :material/title ?s) [[?e ?t _ ?sc]]]]
                  (d/db conn) s))))

;;;; Utilities for adding materials

(defn invert [s]
  (map - s))

(defn dezip [s]
  (let [tuple-size (count (first s))]
    (mapv (fn [n]
            (map #(nth % n) s))
          (range tuple-size))))

;;;; Database stuff for adding materials

(defn get-present-tags [conn tags owner-eid]
  (let [tags-s (set tags)]
    (dezip (d/q '[:find ?e ?t
                  :in $ ?ts ?o
                  :where [?e :tag/owner ?o]
                         [?e :tag/text ?t]
                         [(contains? ?ts ?t)]]
                (d/db conn) tags owner-eid))))

(defn add-tags-txd [conn tags owner-eid]
  (when tags
    (let [[present-tags eids] (get-present-tags conn tags owner-eid)
          tags-to-add (remove (set present-tags) tags)
          tempids (map #(d/tempid :part/bibliography)
                       (invert (range (count tags-to-add))))]
      [(concat eids tempids)
       (mapv (fn [i t]
               {:db/id i
                :tag/text t
                :tag/owner owner-eid})
             tempids tags-to-add)])))

(defn add-material-txd [m tag-eids owner-eid]
  (remove-vals nil? {:db/id #db/id[:part/bibliography]
                     :material/title    (m :title)
                     :material/uri      (m :uri)
                     :material/comments (m :comments)
                     :material/tags     tag-eids
                     :material/owner    owner-eid}))

(defn save-material
  "Add material described by m to the database. Since currently multi-user
  capabilities are note implemented, the owner will be sandbox." ; ->LIVE-SPEC
  [conn m]
  (let [db (d/db conn)
        sandbox-eid (d/q '[:find ?e . :where [?e :user/username "sandbox"]] db)
        [tag-eids tags-txd] (add-tags-txd conn (m :tags) sandbox-eid)
        mat-txd (add-material-txd m tag-eids sandbox-eid)]
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
