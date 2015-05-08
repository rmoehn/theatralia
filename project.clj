(defproject theatralia "0.1.2-SNAPSHOT"
  ;; Note that I'm too lazy to update this version number everytime I tag a
  ;; commit in order to be deployed to the web server. See also the README.
  ;; Umm, maybe I should read this stuff about semantic versioning, though.
  ;; TODO: Read about semantic versioning.
  :description "A research notebook web application"
  :url "https://github.com/rmoehn/theatralia"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env/datomic_username]
                                   :password [:gpg :env/datomic_passphrase]}}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [medley "0.5.5"]
                 [com.stuartsierra/component "0.2.2"]
                 [slingshot "0.12.2"]
                 [dire "0.5.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-rc1"]
                 [om-sync "0.1.1"]
                 [ring "1.3.1"]
                 [fogus/ring-edn "0.2.0"]
                 [compojure "1.2.1"]
                 [garden "1.2.5"]
                 [enlive "1.1.5"]
                 [kioo "0.4.1-20150314.121103-8" :exclusions [org.clojure/clojure]]
                 [com.datomic/datomic-pro "0.9.5130" :exclusions [joda-time]]
                 [beckon "0.1.1"]]

  :node-dependencies [[source-map-support "0.2.8"]]

  :plugins [[lein-ring "0.8.13"]
            [lein-cljsbuild "1.0.5"]
            [lein-npm "0.4.0"]]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]

  :clean-targets  [:target-path

                  ;; The following can't be :clean-targets, because they would
                  ;; get deleted before they could be packed into the uberjar.
                  ;; Delete them manually if you need.
                  ;"resources/public/js/out"
                  ;"resources/public/js/repl-out"
                  ;"resources/public/js/main.js"
                  ;"resources/public/js/main.min.js"
                  ]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[midje "1.6.3" :exclusions [org.clojure/clojure]]
                    [org.clojure/test.check "0.7.0"]
                    [org.clojure/tools.namespace "0.2.7"]]
                   :plugins
                   [[lein-midje "3.1.3"]]}
             :uberjar {:main theatralia.core
                       :aot [theatralia.core]}}

  :jar-name "theatralia-%s-incomplete.jar"
  :uberjar-name "theatralia-%s.jar"

  :hooks [leiningen.cljsbuild]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs"] ; Do we need target/classes?
              :notify-command ["notify-send"]
              :compiler {
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}
             {:id "release"
              :source-paths ["src/cljs"] ; Do we need target/classes?
              :compiler {
                :output-to "resources/public/js/main.min.js"
                :pretty-print false
                :optimizations :advanced}}]})
