(ns theatralia.core
  "Some stuff introduced by the Leiningen Midje template.

   Will be removed or substituted once I've set up testing.")
;; TODO: Remove or substitute.

(defn first-element [sequence default]
  (if (seq sequence)
    (first sequence)
    default))

