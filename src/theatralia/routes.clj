(ns theatralia.routes
  (:require [compojure.core :as cj]
            [compojure.route :as route]))

;;; Credits:
;;;  - https://github.com/weavejester/compojure/

(cj/defroutes routes
  (cj/GET "/" [] "<h1>Hello World</h1>")
  (route/not-found "<h1>Page not found</h1>"))

(defrecord Routes [handler])

(defn make-routes []
  (Routes. routes))
