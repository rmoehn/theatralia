(ns theatralia.style.fonts
  "Weird functions for putting together font loaders for Garden CSS."
  (:require [clojure.string :as string]
            [garden.stylesheet :refer [at-font-face]]))

(defn- make-css-map-for-style [& args]
  (zipmap args [{} {:font-weight "bold"} {:font-style "italic"}]))

(defn- build-font-css [family-name fonts-path font-prefix font-suffix
                       css-map-for-style styles-to-build]
  (map
    (fn [style]
      (let [full-name (str font-prefix (string/join style) font-suffix)
            path (str fonts-path "/" full-name)
            style-map (apply merge (map css-map-for-style (seq style)))]
        (at-font-face (merge {:font-family family-name
                              :src (str "url(" path ")")}
                             style-map))))
    styles-to-build))

(def font-declaration
  (let [ll-map (make-css-map-for-style \R \B \I)
        djv-map (make-css-map-for-style "" "Bold" "Oblique")]
    {:linux-libertine
     (build-font-css "my-serif" "/fonts" "LinLibertine_" ".woff"
                     ll-map ["R" "RB" "RI" "RBI"])
     :linux-biolinum
     (build-font-css "my-sans" "/fonts" "LinBiolinum_" ".woff"
                     ll-map ["R" "RB" "RI"])
     :theano-didot
     (build-font-css "my-serif" "/fonts/theano-didot" "theanodidot-"
                     "-webfont.woff" (make-css-map-for-style "regular")
                     [["regular"]])
     :chivo
     (build-font-css "my-sans" "/fonts/chivo" "chivo-" "-webfont.woff"
                     (make-css-map-for-style "regular" "black" "italic")
                     [["regular"] ["black"] ["black" "italic"]])
     :dejavu
     (build-font-css "my-sans" "/fonts" "DejaVuSans-" "-webfont.woff"
                     djv-map [[""] ["Bold"] ["Oblique"] ["Bold" "Oblique"]])}))
