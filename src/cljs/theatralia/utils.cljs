(ns theatralia.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]))

;; Credits: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent
;; TODO: Fix it in cemerick.url. (RM 2015-07-02)
(defn url-encode [s]
  (apply str
         (map (fn [c] (if (contains? #{\! \' \( \) \*} c)
                        (str \% (-> c (.charCodeAt 0) (.toString 16)))
                        c))
              (url/url-encode s))))
