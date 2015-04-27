(ns theatralia.main
  "Assembles systems for various uses. For development at the REPL we have
  (make-system). The other things are needed by the Leiningen Ring plugin for
  creating a WAR file that can be fed into Tomcat, for example. See the :ring
  section in project.clj.

  Note that at the moment I'm not sure anymore how all this works. I'll have to
  look into it again when I set up continuous integration and perhaps automatic
  deployment."
  (:require [com.stuartsierra.component :as component]
            [theatralia.database.component :refer [make-database]]
            [theatralia.web-server :refer [make-web-server]]
            [theatralia.routes :refer [make-routes]]))

;;; Credits:
;;;  - http://youtu.be/13cmHf_kt-Q from around 31:00
;;;  - https://github.com/stuartsierra/component
;;;  - http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

(defn make-system [config]
  (component/system-map
    :web-app (make-routes)
    :web-server (make-web-server (:web-server-port config))
    :database (make-database (:database-uri config))))
