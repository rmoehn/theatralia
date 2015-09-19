(ns theatralia.views
  (:require-macros [reagent.ratom :refer [reaction]]
                   [plumbing.core :refer [fn->>]])
  (:require [cljs-uuid-utils.core :as uuid]
            kioo.core ; so that kioo/component won't cause warnings
            [kioo.reagent :as kioo :include-macros true]
            [plumbing.core :as plumbing :refer [safe-get]]
            [re-frame.core :as rf]
            [re-frame.utils :as rf-utils]))

;;; Credits: https://github.com/ckirkendall/kioo

;;;; Various helpers

(defn value
  "Returns the value of the input field whose change caused the EVENT."
  [event]
  (-> event .-target .-value))


;;;; Fns for dealing with the key-value area of the database

;; Instead of a random SQUUID we could also take an argument to use as prefix
;; and then a random number.
;; TODO: Make this handle/ratom thing a proper data structure instead of an
;;       ad-hoc vector. (RM 2015-09-02)
(defn get-kv-area
  "Adds an empty key-value area to the app-db and returns a pair [handle ratom].

  handle is a handle for the key-value area. It has at least one attribute,
  :kv-area/handle, a unique key which identifies it. The keys have to be
  keywords. You can put arbitrary values in it. If you get problems with
  values that are collections, have a look at
  https://github.com/tonsky/datascript/issues/69 and at the mayhem around commit
  078546e9.

  ratom is a reactive atom holding a map with the contents of the key-value
  area."
  []
  (let [handle (uuid/make-random-squuid)]
    (rf/dispatch-sync [:kv-area/new handle])
    [handle (rf/subscribe [:kv-area/as-map handle])]))

(defn dispatch-kv
  "Convenience fn around rf/dispatch. Suppose k is what (get-kv-area)
  returned. Instead of executing (rf/dispatch [some-request-id (first k) param1
  …), you can pass the whole k like this: (dispatch-kv [some-request-id k
  param1 …)."
  [[request-id [kv-handle _ :as kva] & other]]
  (rf/dispatch (into [request-id kv-handle] other)))


;;;; Fn for dealing with text input fields

(defn bind-to-kv-area
  "Binds a text input field to the given kv-area in the app-db."
  [[kv-handle kv-ratom]]
  (fn [node]
    (let [id (keyword (plumbing/safe-get-in node [:attrs :id]))
          attrs
          [:value (get @kv-ratom id "")
           :onChange #(rf/dispatch [:kv-area/set kv-handle id (value %)])]]
      ((apply kioo/set-attr attrs) node))))


;;;; Search view

(defn result-item
  "One item of the material search results."
  [[id title _]]
  (kioo/component "templates/sandbox.html"
    [:#search-results :> :ol :> first-child]
    {[:li] (kioo/do-> (kioo/content title)
                      (kioo/set-attr :key id))}))

(defn result-view
  "Material search results."
  []
  (let [results-ra (rf/subscribe [:search/result])]
    (fn []
      (kioo/component "templates/sandbox.html" [:#search-results]
        {[:ol] (kioo/content (map result-item @results-ra))}))))

;; TODO: Show a warning when the search string begins with ? or *. – They are
;;       not allowed in full text queries. On the other hand: we are not going
;;       to use Datomic full text search as our search anyway. (RM 2015-09-10)
;; TODO: Prevent empty searches. (RM 2015-09-10)
(defn search-view
  "A group of components for searching materials."
  []
  (let [kv-area (get-kv-area)]
    (fn search-view-infn []
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput]
         (kioo/do->
           (bind-to-kv-area kv-area)
           (kioo/set-attr :onKeyDown #(when (= (.-key %) "Enter")
                                        (dispatch-kv [:search/submitted kv-area]))))

         [:#submit]
         (kioo/set-attr :onClick #(dispatch-kv [:search/submitted
                                                kv-area]))}))))


;;; View for adding materials

;; MAYBE DE-HACK: Didn't we want to have dump callbacks? Abolish this and make
;;                an event handler that decides what to do. (RM 2015-09-08)
(defn- on-tag-input-change
  "onChange callback for tag input elements."
  [s-id prev-s e]
  (let [s (value e)]
    (cond
      (and (empty? prev-s) (seq s))
      (do
        (rf/dispatch [:tags/add-empty s-id])
        (rf/dispatch [:tags/set s-id s]))

      (and (seq prev-s) (empty? s))
      (rf/dispatch [:tags/remove s-id])

      :else
      (rf/dispatch [:tags/set s-id s]))))

(defn tag-view
  "Renders a single tag input element."
  [[serial-id tag]]
  (let [input-id (str "newMatTagInput" serial-id)]
    (kioo/component "templates/sandbox.html" [:.form-inline :> first-child]
      {[:*] (kioo/set-attr :key serial-id)

       [:label]
       (kioo/do-> (kioo/set-attr :for input-id)
                  (kioo/content (str "Tag with ID " serial-id)))

       [:input]
       (kioo/set-attr :id input-id
                      :value tag
                      :onChange #(on-tag-input-change serial-id tag %))})))

;; TODO: Investigate the behaviour that keystrokes get lost when starting to
;;       type very rapidly on a new tag input. (RM 2015-08-30)
;; TODO: Prevent focus loss when a tag input becomes empty. For some reason it
;;       doesn't always occur, though. (RM 2015-08-30)
(defn tag-inputs-view
  "Renders the tag input elements."
  []
  (let [tags-ra (rf/subscribe [:add-material/tags])
        next-id (fn->> (map first) (reduce max -1) inc)
        with-empty (reaction (conj (vec @tags-ra)
                                   [(next-id @tags-ra) ""]))]
    (fn tag-inputs-view-infn []
      (kioo/component "templates/sandbox.html" [:#tag-list-group]
        {[:.form-inline]
         (kioo/content (map tag-view @with-empty))}))))

;; TODO: On submit we have to remove duplicate tags. (RM 2015-08-26)
(defn add-material-view
  "Renders to a form through which the user can input new tags."
  []
  (let [kv-area (get-kv-area)]
    (fn add-material-view-infn []
      (kioo/component "templates/sandbox.html" [:#add-material-form]
        {[:#newMatSubmit]
         (kioo/set-attr
           :onClick (fn [e]
                      (.preventDefault e)
                      (dispatch-kv [:add-material/submit kv-area])))
         [:.simple-input] (bind-to-kv-area kv-area)
         [:#tag-list-group] (kioo/substitute [tag-inputs-view])}))))


;;; Root view

(defn root-view
  "The application's high-level structure."
  []
  (kioo/component "templates/sandbox.html"
    {[:#search-field] (kioo/substitute [search-view])
     [:#search-results] (kioo/substitute [result-view])
     [:#add-material-form] (kioo/substitute [add-material-view])}))
