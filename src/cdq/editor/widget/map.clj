(ns cdq.editor.widget.map
  (:require [cdq.editor-window :as editor-window]
            [cdq.schema :as schema]
            [cdq.editor.widget.map.helper :as helper]
            [cdq.malli :as m]
            [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.set :as set]
            [clojure.vis-ui.separator :as separator]
            [clojure.vis-ui.widget :as widget]))

(defn- k->default-value [schemas k]
  (let [schema (utils/safe-get schemas k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/get-type schema)) nil
     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button
     :else (m/generate (schema/malli-form schema schemas)
                       {:size 3}))))

(defn- vertical-separator-cell []
  {:actor (separator/vertical)
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(defn- component-row [editor-widget k schema schemas table]
  [(helper/label-cell {:display-remove-component-button? (m/optional? k (schema/malli-form schema schemas))
                       :k k
                       :table table
                       :label-text (helper/k->label-text k)})
   (vertical-separator-cell)
   {:actor editor-widget
    :left? true}])

(defn- open-add-component-window! [{:keys [ctx/db
                                           ctx/stage]}
                                   schema
                                   map-widget-table]
  (let [schemas (:schemas db)
        window (widget/window {:title "Choose"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (schema/value schema nil map-widget-table schemas)))
                                   (m/map-keys (schema/malli-form schema schemas))))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [(widget/text-button (name k)
                            (fn [_actor ctx]
                              (.remove window)
                              (table/add-rows! map-widget-table [(component-row (schema/build ctx
                                                                                              (get schemas k)
                                                                                              k
                                                                                              (k->default-value schemas k))
                                                                                k
                                                                                schema
                                                                                schemas
                                                                                map-widget-table)])
                              (editor-window/rebuild! ctx)))]))
    (.pack window)
    (stage/add! stage window)))

(defn- horiz-sep [colspan]
  (fn []
    [{:actor (separator/horizontal)
      :pad-top 2
      :pad-bottom 2
      :colspan colspan
      :fill-x? true
      :expand-x? true}]))

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defn create [schema  _attribute m {:keys [ctx/db
                                           ctx/config] :as ctx}]
  (let [k-sort-order (:property-k-sort-order (:cdq.editor.widget.map config))
        table (widget/table
               {:cell-defaults {:pad 5}
                :id :map-widget})
        colspan 3
        component-rows (interpose-f (horiz-sep colspan)
                                    (map (fn [[k v]]
                                           (component-row (schema/build ctx (get (:schemas db) k) k v)
                                                          k
                                                          schema
                                                          (:schemas db)
                                                          table))
                                         (utils/sort-by-k-order k-sort-order m)))
        opt? (seq (set/difference (m/optional-keyset (schema/malli-form schema (:schemas db)))
                                  (set (keys m))))]
    (table/add-rows!
     table
     (concat [(when opt?
                [{:actor (widget/text-button "Add component"
                                             (fn [_actor ctx]
                                               (open-add-component-window! ctx schema table)))
                  :colspan colspan}])]
             [(when opt?
                [{:actor (separator/horizontal)
                  :pad-top 2
                  :pad-bottom 2
                  :colspan colspan
                  :fill-x? true
                  :expand-x? true}])]
             component-rows))
    table))

(defn value [_  _attribute table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (schema/value (get schemas k) k widget schemas)])))
