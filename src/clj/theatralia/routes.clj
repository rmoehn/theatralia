(ns theatralia.routes
  (:require [compojure.core :as cj]
            [compojure.route :as route]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [theatralia.welcome-page :as wp]))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

;;; TODO: Move this to the appropriate namespace.
(defn init []
  (generate-response
    {:last-input {:url "/last-input" :coll {:text "Hello World!"}}}))

(cj/defroutes routes
  (cj/GET "/" [] (wp/index))
  (cj/GET "/init" [] (init))
  (cj/PUT "/last-input" {params :edn-params} (println (:text params)))
  (cj/GET "/main.css" [] (wp/main-css))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defrecord Routes [handler])

(defn make-routes []
  (Routes. (wrap-edn-params routes)))
