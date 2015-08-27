(ns theatralia.views
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [cljs-uuid-utils.core :as uuid]
            [kioo
             core ; so that kioo/component won't cause warnings
             [reagent :as kioo :include-macros true]]
            [plumbing.core :as plumbing :refer [safe-get]]
            [re-frame.core :as rf]))

;;;; Various helpers

(defn value
  "Returns the value of the input field whose change caused the
  TEXT-CHANGE-EVENT."
  [text-change-event]
  (-> text-change-event .-target .-value))


;;;; Fns for dealing with the scratch part of the database

;; Instead of a random SQUUID we could also take an argument to use as prefix
;; and then a random number. Or just the provided scratch-key, but we'd have to
;; do check for collisions, which could be ugly.
(defn get-scratch
  "Adds a scratch entity to the app-db and returns a pair [eid ratom].

  eid is the ID of the scratch entity. It has at least one attribute,
  :scratch/key, a unique key which identifies it. Being a scratch space, you can
  attach arbitrary other attributes to it. If you get problems with attribute
  values that are collections, have a look at
  https://github.com/tonsky/datascript/issues/69 and at the mayhem around commit
  078546e9.

  ratom is a reactive atom holding all the attributes of the scratch entity."
  []
  (let [scratch-key (uuid/make-random-squuid)]
    (rf/dispatch-sync [:new-scratch scratch-key])
    (let [scratch-entid @(rf/subscribe [:get-scratch-entid scratch-key])

          scratch-contents-ra
          (rf/subscribe [:get-scratch-contents scratch-entid])]
      [scratch-entid scratch-contents-ra])))

(defn dispatch-scratch
  "Convenience fn around rf/dispatch. Suppose s is what (get-scratch) returned.
  Instead of executing (rf/dispatch [some-request-id (first s) param1 …), you
  can pass the whole s like this: (dispatch-scratch [some-request-id s param1
  …)."
  [[request-id [scratch-entid _] & other]]
  (rf/dispatch (into [request-id scratch-entid] other)))


;;;; Fn for dealing with text input fields

(defn bind-and-set-attr
  "Binds a text input field to the given scratch space in the app-db and adds
  the ATTRS to its existing attributes."
  [[scratch-id scratch-ratom] & attrs]
  {:pre [(even? (count attrs))]}
  (fn [node]
    (let [id (keyword (plumbing/safe-get-in node [:attrs :id]))
          default-attrs
          [:value (get @scratch-ratom id "")
           :onChange #(rf/dispatch [:set-scratch-val scratch-id id (value %)])]]
      ((apply kioo/set-attr
              (concat default-attrs attrs)) node))))


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
  (let [results-ra (rf/subscribe [:search-result])]
    (fn []
      (kioo/component "templates/sandbox.html" [:#search-results]
        {[:ol] (kioo/content (map result-item @results-ra))}))))

(defn search-view
  "A group of components for searching materials."
  []
  (let [scratch (get-scratch)]
    (fn search-view-infn []
      (kioo/component "templates/sandbox.html" [:#search-field]
        {[:#searchInput]
         (bind-and-set-attr
           scratch
           :onKeyDown
           #(when (= (.-key %) "Enter")
              (dispatch-scratch [:search-submitted scratch])))

         [:#submit]
         (kioo/set-attr :onClick #(dispatch-scratch [:search-submitted
                                                     scratch]))}))))

;;; View for adding materials

(defn tag-view [[index tag]]
  (let [input-id (str "newMatTagInput" index)]
    (kioo/component "templates/sandbox.html" [:#existing-tags :> first-child]
      {[:label]
       (kioo/do-> (kioo/set-attr :for input-id)
                  (kioo/content (str "Tag " index)))

       [:input]
       (kioo/set-attr :id input-id
                      :value tag
                      :onChange #(rf/dispatch [:tag-change index (value %)])
                      :onFocus #(when (empty? (value %))
                                  (rf/dispatch [:new-tag index]))
                      :onBlur #(when (empty? (value %))
                                 (rf/dispatch [:remove-tag index])))})))

(defn tag-inputs-view []
  (let [tags-ra (rf/subscribe [:tags])
        with-empty (reaction (-> @tags-ra
                                 vec
                                 (conj [(count @tags-ra) ""])))]
    (fn tag-inputs-view-infn []
      (kioo/component "templates/sandbox.html" [:#tag-list-group]
        {[:#existing-tags]
         (kioo/content (map tag-view @with-empty))

         #_[:#empty-tag :input]
         #_(kioo/set-attr :onChange #(rf/dispatch [:new-tag (value %)]))}))))

;; TODO: On submit we have to remove duplicate tags. (RM 2015-08-26)
(defn add-material-view []
  (let [scratch (get-scratch)]
    (fn add-material-view-infn []
      (kioo/component "templates/sandbox.html" [:#add-material-form]
        {[:#newMatSubmit]
         (kioo/set-attr
           :onClick (fn [e]
                      (.preventDefault e)
                      (dispatch-scratch [:add-material scratch])))
         [:.simple-input] (bind-and-set-attr scratch)
         [:#tag-list-group] (kioo/substitute [tag-inputs-view])}))))

;;; Root view

(defn root-view
  "The application's high-level structure."
  []
  (kioo/component "templates/sandbox.html"
    {[:#search-field] (kioo/substitute [search-view])
     [:#search-results] (kioo/substitute [result-view])
     [:#add-material-form] (kioo/substitute [add-material-view])}))

