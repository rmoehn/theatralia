(ns theatralia.core)

(defn first-element [sequence default]
  (if (seq sequence)
    (first sequence)
    default))

