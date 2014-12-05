(defproject theatralia "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [ring "1.3.1"]
                 [compojure "1.2.1"]]
  :plugins [[lein-ring "0.8.13"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[midje "1.6.3" :exclusions [org.clojure/clojure]]
                    [org.clojure/tools.namespace "0.2.7"]]
                   :plugins
                   [[lein-midje "3.1.3"]]}}
  :ring {:init theatralia.main/start-production-system
         :handler theatralia.main/production-handler
         :destroy theatralia.main/stop-production-system
         :uberwar-name theatralia.war})
