(defproject theatralia "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:dev {:dependencies
                   [[midje "1.6.3" :exclusions [org.clojure/clojure]]]
                   :plugins
                   [[lein-midje "3.1.3"]]}})
