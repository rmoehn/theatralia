(ns theatralia.routes
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as cj]
            [compojure.route :as route]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [datomic.api :as d]
            [theatralia.welcome-page :as wp]))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn search-for [conn s]
  (generate-response
    (sort-by #(nth % 2) >
             (d/q '[:find ?e ?t ?sc
                    :in $ ?s
                    :where [(fulltext $ :material/title ?s) [[?e ?t _ ?sc]]]]
                  (d/db conn) s))))

(defn make-handler [routes]
  (-> (let [conn (:conn (:database routes))]
        (cj/routes
          (cj/GET "/" [] (wp/index))
          (cj/GET "/gq/:s" [s] (search-for conn s))
          (cj/POST "/materials" {params :edn-params}
                                (do (println params)
                                    (generate-response {:ok :ok})))
          (cj/GET "/main.css" [] (wp/main-css))
          (route/resources "/")
          (route/not-found "<h1>Page not found</h1>")))
      wrap-edn-params))

(defrecord Routes [database])

(defn make-routes []
  (component/using (map->Routes {})
                   [:database]))
