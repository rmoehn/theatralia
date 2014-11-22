(defproject theatralia "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [ring "1.3.1"]
                 [compojure "1.2.1"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[midje "1.6.3" :exclusions [org.clojure/clojure]]
                    [org.clojure/tools.namespace "0.2.3"]]
                   :plugins
                   [[lein-midje "3.1.3"]]}})
