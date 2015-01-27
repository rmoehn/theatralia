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

(defn last-input [conn]
  "Returns a tuple of the ID of the entity with the :last-input/text attribute
  and the value of that attribute."
  (first (d/q '[:find ?e ?t :where [?e :last-input/text ?t]]
              (d/db conn))))

(defn init [conn]
  (let [[_ text] (last-input conn)
        default "Hello World!"]
    (when (nil? text)
      @(d/transact conn
                   [[:db/add #db/id[:db.part/user] :last-input/text default]]))
    (generate-response
      {:last-input {:url "/last-input"
                    :coll {:text (or text default)}}})))

(defn update-last-input
  "Takes a request map with a :text key and updates the newest entity with the
  attribute :last-input/text."
  [{:keys [text]} conn]
  (let [[eid _] (last-input conn)]
    @(d/transact conn [[:db/add eid :last-input/text text]]))
  (generate-response {:status :ok}))

(defn make-handler [routes]
  (-> (let [conn (:conn (:database routes))]
        (cj/routes
          (cj/GET "/" [] (wp/index))
          (cj/GET "/init" [] (init conn))
          (cj/GET "/last-input" [] (second (last-input conn)))
          (cj/PUT "/last-input" {params :edn-params}
                  (update-last-input params conn))
          (cj/GET "/main.css" [] (wp/main-css))
          (route/resources "/")
          (route/not-found "<h1>Page not found</h1>")))
      wrap-edn-params))

(defrecord Routes [database])

(defn make-routes []
  (component/using (map->Routes {})
                   [:database]))
