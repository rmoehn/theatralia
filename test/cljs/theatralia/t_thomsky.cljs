(ns theatralia.t-thomsky
  (:require-macros cemerick.cljs.test)
  (:require [cemerick.cljs.test :as t]
            [theatralia.thomsky :as tsky]))

;;; Credits: @#' swearing in order to access private vars is recommended here:
;;; http://dev.clojure.org/display/community/Library+Coding+Standards
;;; Be aware of
;;; https://groups.google.com/d/topic/clojure/Mi277rszUs0/discussion, too, if
;;; you intend to use this in other places.

;; These are most of the queries from http://docs.datomic.com/query.html. Errm,
;; couldn't stop myself…
(t/deftest test-single-valued?
  (t/are [b q] (= b (@#'tsky/single-valued? q))
       true '[:find ?e . :where [?e :attr _]]
       true '{:find [[?year ?month ?day]]
              :in [$ ?name]
              :where [[?artist :artist/name ?name]
                      [?artist :artist/startDay ?day]
                      [?artist :artist/startMonth ?month]
                      [?artist :artist/startYear ?year]]}
       true '[:find [?year ?month ?day]
              :in $ ?name
              :where [?artist :artist/name ?name]
              [?artist :artist/startDay ?day]
              [?artist :artist/startMonth ?month]
              [?artist :artist/startYear ?year]]
       true '[:find ?year .
              :in $ ?name
              :where [?artist :artist/name ?name]
              [?artist :artist/startYear ?year]]
       true '[:find (count ?eid) .
              :where [?eid :artist/name]
              (not [?eid :artist/country :country/CA])]
       true '[:find (count ?artist) .
              :where [?artist :artist/name]
              (not-join [?artist]
                        [?release :release/artists ?artist]
                        [?release :release/year 1970])]
       true '[:find (count ?r) .
              :where [?r :release/name "Live at Carnegie Hall"]
              (not-join [?r]
                        [?r :release/artists ?a]
                        [?a :artist/name "Bill Withers"])]
       true '[:find (count ?medium) .
              :where (or [?medium :medium/format :medium.format/vinyl7]
                         [?medium :medium/format :medium.format/vinyl10]
                         [?medium :medium/format :medium.format/vinyl12]
                         [?medium :medium/format :medium.format/vinyl])]
       true '[:find (count ?artist) .
              :where (or [?artist :artist/type :artist.type/group]
                         (and [?artist :artist/type :artist.type/person]
                              [?artist :artist/gender :artist.gender/female]))]
       true '[:find (count ?release) .
              :where [?release :release/name]
              (or-join [?release]
                       (and [?release :release/artists ?artist]
                            [?artist :artist/country :country/CA])
                       [?release :release/year 1970])]
       true '[:find ?celsius .
              :in ?fahrenheit
              :where [(- ?fahrenheit 32) ?f-32]
              [(/ ?f-32 1.8) ?celsius]]
       true '[:find [?e ?attr ?name]
              :in $ ?e
              :where [(get-some $ ?e :country/name :artist/name) [?attr ?name]]]
       true '[:find (sum ?heads) .
              :in [[_ ?heads]]]
       true '[:find (sum ?heads) .
              :with ?monster
              :in [[?monster ?heads]]]
       true '[:find [(min ?dur) (max ?dur)]
              :where [_ :track/duration ?dur]]
       true '[:find (sum ?count) .
              :with ?medium
              :where [?medium :medium/trackCount ?count]]
       true '[:find (user/mode ?track-count) .
              :with ?media
              :where [?media :medium/trackCount ?track-count]]
       false '[:find ?e ?x
               :where [?e :age 42] [?e :likes ?x]]
       false '[:find ?x
               :where [_ :likes ?x]]
       false '[:find ?release-name
               :in $
               :where [$ _ :release/name ?release-name]]
       false '[:find ?release-name
               :in $ ?artist-name
               :where [?artist :artist/name ?artist-name]
               [?release :release/artists ?artist]
               [?release :release/name ?release-name]]
       false '[:find ?release
               :in $ [?artist-name ?release-name]
               :where [?artist :artist/name ?artist-name]
               [?release :release/artists ?artist]
               [?release :release/name ?release-name]]
       false '[:find ?release-name
               :in $ [?artist-name ...]
               :where [?artist :artist/name ?artist-name]
               [?release :release/artists ?artist]
               [?release :release/name ?release-name]]
       false '[:find ?release
               :in $ [[?artist-name ?release-name]]
               :where [?artist :artist/name ?artist-name]
               [?release :release/artists ?artist]
               [?release :release/name ?release-name]]
       false '[:find ?artist-name ?release-name
               :where [?release :release/name ?release-name]
               [?release :release/artists ?artist]
               [?artist :artist/name ?artist-name]]
       false '[:find [?release-name ...]
               :in $ ?artist-name
               :where [?artist :artist/name ?artist-name]
               [?release :release/artists ?artist]
               [?release :release/name ?release-name]]
       false '[:find ?name ?year
               :where [?artist :artist/name ?name]
               [?artist :artist/startYear ?year]
               [(< ?year 1600)]]
       false '[:find ?track-name ?minutes
               :in $ ?artist-name
               :where [?artist :artist/name ?artist-name]
               [?track :track/artists ?artist]
               [?track :track/duration ?millis]
               [(quot ?millis 60000) ?minutes]
               [?track :track/name ?track-name]]
       false '[:find ?artist-name ?year
               :in $ [?artist-name ...]
               :where [?artist :artist/name ?artist-name]
               [(get-else $ ?artist :artist/startYear "N/A") ?year]]
       false '[:find ?entity ?name ?tx ?score
               :in $ ?search
               :where [(fulltext $ :artist/name ?search) [[?entity ?name ?tx ?score]]]]
       false '[:find ?name
               :where [?artist :artist/name ?name]
               [(missing? $ ?artist :artist/startYear)]]
       false '[:find [?tx ...]
               :in ?log
               :where [(tx-ids ?log 1000 1050) [?tx ...]]]
       false '[:find [?e ...]
               :in ?log ?tx
               :where [(tx-data ?log ?tx) [[?e]]]]
       false '[:find ?k ?v
               :where [(System/getProperties) [[?k ?v]]]]
       false '[:find (count ?name) (count-distinct ?name)
               :with ?artist
               :where [?artist :artist/name ?name]]
       false '[:find ?year (median ?namelen) (avg ?namelen) (stddev ?namelen)
               :with ?track
               :where [?track :track/name ?name]
               [(count ?name) ?namelen]
               [?medium :medium/tracks ?track]
               [?release :release/media ?medium]
               [?release :release/year ?year]]
       false '[:find (pull ?e [:release/name])
               :in $ ?artist
               :where [?e :release/artists ?artist]]
       false '[:find (pull ?e pattern)
               :in $ ?artist pattern
               :where [?e :release/artists ?artist]]
       false '{:find [?e]
               :in [$ ?fname ?lname]
               :where [[?e :user/firstName ?fname]
                       [?e :user/lastName ?lname]]}))
