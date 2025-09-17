(ns cdq.schema.map
  (:require [cdq.ctx :as ctx]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.ui.editor.window :as editor-window]
            [cdq.ui.editor.widget.map.helper :as helper]
            [cdq.malli :as m]
            [clojure.utils :as utils]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.ui.table :as table]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]
            [clojure.set :as set]
            [clojure.vis-ui.separator :as separator]))

(defn malli-form [[_ ks] schemas]
  (schemas/create-map-schema schemas ks))

(defn create-value [_ v db]
  (schemas/build-values (:schemas db) v db))

(defn- build-widget [ctx schema k v]
  (let [widget (schema/create schema v ctx)
        widget (if (instance? com.badlogic.gdx.scenes.scene2d.Actor widget)
                 widget
                 (scene2d/build widget))]
    ; FIXME assert no user object !
    (actor/set-user-object! widget [k v])
    widget))

(defn- component-row [editor-widget k schema schemas table]
  (helper/component-row
   {:editor-widget editor-widget
    :display-remove-component-button? (m/optional? k (schema/malli-form schema schemas))
    :k k
    :table table
    :label-text (helper/k->label-text k)}))

(defn- open-add-component-window! [{:keys [ctx/db
                                           ctx/stage]}
                                   schema
                                   map-widget-table]
  (let [schemas (:schemas db)
        window (scene2d/build {:actor/type :actor.type/window
                               :title "Choose"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (schema/value schema map-widget-table schemas)))
                                   (m/map-keys (schema/malli-form schema schemas))))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [{:actor {:actor/type :actor.type/text-button
                 :text (name k)
                 :on-clicked (fn [_actor ctx]
                               (.remove window)
                               (table/add-rows! map-widget-table [(component-row (build-widget ctx
                                                                                               (get schemas k)
                                                                                               k
                                                                                               (schemas/default-value schemas k))
                                                                                 k
                                                                                 schema
                                                                                 schemas
                                                                                 map-widget-table)])
                               (ctx/handle-txs! ctx [[:tx/rebuild-editor-window]]))}}]))
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

(defn create
  [schema
   m
   {:keys [ctx/db
           ctx/editor]
    :as ctx}]
  (let [k-sort-order (:editor/property-k-sort-order editor)
        schemas (:schemas db)
        opt? (seq (set/difference (m/optional-keyset (schema/malli-form schema schemas))
                                  (set (keys m))))
        k->widget (into {}
                        (for [[k v] m]
                          [k (build-widget ctx (get schemas k) k v)]))
        table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}
                :actor/name "cdq.schema.map.ui.widget"})
        colspan 3
        component-rows (interpose-f (horiz-sep colspan)
                                    (map (fn [[k _v]]
                                           (component-row (k->widget k)
                                                          k
                                                          schema
                                                          schemas
                                                          table))
                                         (utils/sort-by-k-order k-sort-order m)))]
    (table/add-rows!
     table
     (concat [(when opt?
                [{:actor {:actor/type :actor.type/text-button
                          :text "Add component"
                          :on-clicked (fn [_actor ctx]
                                        (open-add-component-window! ctx schema table))}
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

(defn value [_ table schemas]
  (editor-window/map-widget-property-values table schemas))
