(ns theatralia.readers
  "Various functions for reading tagged literals. See also
  src/clj/data_readers.clj.")

(defn read-uri [s]
  (java.net.URI. s))
