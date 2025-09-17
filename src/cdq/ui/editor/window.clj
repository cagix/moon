(ns cdq.ui.editor.window
  (:require [cdq.ctx :as ctx]
            [cdq.malli :as m]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.ui.editor.widget.map.helper :as helper]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.vis-ui.separator :as separator]))

(defn create
  [{:keys [actors rows]}]
  {:actor/type :actor.type/window
   :title "[SKY]Property[]"
   :actor/name "cdq.ui.editor.window"
   :modal? true
   :close-button? true
   :center? true
   :close-on-escape? true
   :group/actors actors
   :rows rows
   :cell-defaults {:pad 5}
   :pack? true})

(defn build-widget [ctx schema k v]
  (let [widget (schema/create schema v ctx)
        widget (if (instance? com.badlogic.gdx.scenes.scene2d.Actor widget)
                 widget
                 (scene2d/build widget))]
    ; FIXME assert no user object !
    (actor/set-user-object! widget [k v])
    widget))

(defn- component-row [editor-widget k optional-key? table]
  (helper/component-row
   {:editor-widget editor-widget
    :display-remove-component-button? optional-key?
    :k k
    :table table
    :label-text (helper/k->label-text k)}))

(defn- add-component-window [schemas schema map-widget-table]
  (let [window (scene2d/build {:actor/type :actor.type/window
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
                                                                                 (m/optional? k (schema/malli-form schema schemas))
                                                                                 map-widget-table)])
                               (ctx/handle-txs! ctx [[:tx/rebuild-editor-window]]))}}]))
    (.pack window)
    window))

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

(defn map-widget-table
  [{:keys [schema
           k->widget
           k->optional?
           ks-sorted
           opt?]}]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}
                :actor/name "cdq.schema.map.ui.widget"})
        colspan 3
        component-rows (interpose-f (horiz-sep colspan)
                                    (map (fn [k]
                                           (component-row (k->widget k)
                                                          k
                                                          (k->optional? k)
                                                          table))
                                         ks-sorted))]
    (table/add-rows!
     table
     (concat [(when opt?
                [{:actor {:actor/type :actor.type/text-button
                          :text "Add component"
                          :on-clicked (fn [_actor {:keys [ctx/db
                                                          ctx/stage]}]
                                        (stage/add! stage (add-component-window (:schemas db) schema table)))}
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

(defn map-widget-property-values [table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (schema/value (get schemas k) widget schemas)])))
