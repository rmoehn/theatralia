(ns theatralia.routes
  (:require [compojure.core :as cj]
            [compojure.route :as route]
            [theatralia.welcome-page :as wp]))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

(cj/defroutes routes
  (cj/GET "/" [] (wp/index))
  (cj/GET "/main.css" [] (wp/main-css))
  (route/not-found "<h1>Page not found</h1>"))

(defrecord Routes [handler])

(defn make-routes []
  (Routes. routes))
