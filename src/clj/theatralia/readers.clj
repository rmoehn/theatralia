(ns theatralia.readers)

(defn read-uri [s]
  (java.net.URI. s))
