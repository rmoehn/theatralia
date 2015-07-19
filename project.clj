(defproject theatralia "0.1.2-SNAPSHOT"
  ;; Note that I'm too lazy to update this version number everytime I tag a
  ;; commit in order to be deployed to the web server. See also the README.
  ;; TODO: Think about deployment and versioning strategy.
  :description "A research notebook web application"
  :url "https://github.com/rmoehn/theatralia"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env/datomic_username]
                                   :password [:gpg :env/datomic_passphrase]}}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [medley "0.6.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [com.taoensso/timbre "3.4.0"]
                 [slingshot "0.12.2"]
                 [dire "0.5.3"]
                 [prismatic/plumbing "0.4.4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.0"]
                 [re-frame "0.4.1"]
                 [ring "1.3.2"]
                 [fogus/ring-edn "0.3.0"]
                 [compojure "1.3.4"]
                 [garden "1.2.5"]
                 [enlive "1.1.5"]
                 [kioo "0.4.1-20150314.121103-8" :exclusions [org.clojure/clojure]]
                 [com.cemerick/url "0.1.1"]
                 [cljs-ajax "0.3.13"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.datomic/datomic-pro "0.9.5130" :exclusions [joda-time]]
                 [datascript "0.11.5"]
                 [beckon "0.1.1"]]

  :plugins [[lein-ring "0.9.4"]
            [lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.3"]]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]

  ;; REVIEW: Not sure if listing all these paths is necessary. Shouldn'
  ;;         resources/public/js be enough? (RM 2015-07-19)
  :clean-targets  ^{:protect false} [:target-path
                                     "resources/public/js/out"
                                     "resources/public/js/main.js"
                                     "resources/public/js/main.min.js"
                                     "resources/public/js/test"
                                     "resources/public/js/test/frontend-test.js"]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[midje "1.6.3" :exclusions [org.clojure/clojure]]
                    [org.clojure/test.check "0.7.0"]
                    [org.clojure/tools.namespace "0.2.10"]]
                   :plugins
                   [[lein-midje "3.1.3"]]}
             :uberjar {:main theatralia.core
                       :aot [theatralia.core]}}

  :jar-name "theatralia-%s-slim.jar"
  :uberjar-name "theatralia-%s.jar"

  :hooks [leiningen.cljsbuild]

  :cljsbuild
  {:builds [{:id "dev"
              :source-paths ["src/cljs"]
              :figwheel true
              :compiler {
                ; Figwheel-specific things
                :main theatralia.core
                :asset-path "/js/out" ; Base URL for JS requests from browser.
                :recompile-dependents true ; Speed doesn't matter right now.

                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}
             {:id "release"
              :source-paths ["src/cljs"]
              :compiler {
                :output-to "resources/public/js/main.min.js"
                :pretty-print false
                :optimizations :advanced}}
             {:id "test"
              :source-paths  ["src/cljs" "test/cljs"]
              :compiler  {:pretty-print true
                          :optimizations :none
                          :output-to "resources/public/js/test/frontend-test.js"
                          :output-dir "resources/public/js/test"
                          :source-map "resources/public/js/test/sourcemap-test.js"}}]
   :test-commands {"frontend-unit-tests"
                   ["node_modules/karma/bin/karma"
                    "start" "karma.test.conf.js" "--single-run"]}}

  :repl-options {:timeout 180000})
