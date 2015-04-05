(ns theatralia.core
  "Entry point for running with java -jar.

  The way I've configured it in project.clj, running lein uberjar, java -jar
  <generated-jar> will look for the -main function in this class to run the
  system.

  The other stuff was introduced by the Leiningen Midje template and will
  probably be removed once I've set up testing."
  (:require [com.stuartsierra.component :as component]
            [theatralia.main :as main])
  (:gen-class))
;; TODO: Remove or substitute.

(defn first-element [sequence default]
  (if (seq sequence)
    (first sequence)
    default))

(defn -main [& args]
  (component/start
    (main/make-system
      {:web-server-port 17777
       :database-uri "datomic:dev://localhost:4334/theatralia"})))
