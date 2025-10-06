(ns cdq.scene2d.build.map-widget-table
  (:require [clojure.malli :as m]
            [cdq.ui.editor.schema]
            [cdq.db.schema :as schema]
            [cdq.db.schemas :as schemas]
            [cdq.ui.editor.value-widget :as value-widget]
            [cdq.ui.editor.map-widget-table :as map-widget-table]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [com.badlogic.gdx.scenes.scene2d.ui.widget-group :as widget-group]))

(defn- rebuild!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (-> stage
                   stage/root
                   (group/find-actor "cdq.ui.editor.window"))
        map-widget-table (-> window
                             (group/find-actor "cdq.ui.widget.scroll-pane-table")
                             (group/find-actor "scroll-pane-table")
                             (group/find-actor "cdq.db.schema.map.ui.widget"))
        property (map-widget-table/get-value map-widget-table (:db/schemas db))]
    (actor/remove! window)
    (stage/add! stage
                (scene2d/build
                 {:actor/type :actor.type/editor-window
                  :ctx ctx
                  :property property}))))

(defn- k->label-text [k]
  (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
  )

(defn- component-row*
  [{:keys [editor-widget
           display-remove-component-button?
           k
           table
           label-text]}]
  [{:actor {:actor/type :actor.type/table
            :cell-defaults {:pad 2}
            :rows [[{:actor (when display-remove-component-button?
                              {:actor/type :actor.type/text-button
                               :text "-"
                               :on-clicked (fn [_actor ctx]
                                             (actor/remove! (first (filter (fn [actor]
                                                                             (and (actor/user-object actor)
                                                                                  (= k ((actor/user-object actor) 0))))
                                                                           (group/children table))))
                                             (rebuild! ctx))})
                     :left? true}
                    {:actor {:actor/type :actor.type/label
                             :label/text label-text}}]]}
    :right? true}
   {:actor {:actor/type :actor.type/separator-vertical}
    :pad-top 2
    :pad-bottom 2
    :fill-y? true
    :expand-y? true}
   {:actor editor-widget
    :left? true}])

(defn- component-row [editor-widget k optional-key? table]
  (component-row*
   {:editor-widget editor-widget
    :display-remove-component-button? optional-key?
    :k k
    :table table
    :label-text (k->label-text k)}))

(defn- add-component-window [schemas schema map-widget-table]
  (let [window (scene2d/build {:actor/type :actor.type/window
                               :title "Choose"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (cdq.ui.editor.schema/value schema map-widget-table schemas)))
                                   (m/map-keys (schema/malli-form schema schemas))))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [{:actor {:actor/type :actor.type/text-button
                 :text (name k)
                 :on-clicked (fn [_actor ctx]
                               (actor/remove! window)
                               (table/add-rows! map-widget-table [(component-row (value-widget/build ctx
                                                                                                     (get schemas k)
                                                                                                     k
                                                                                                     (schemas/default-value schemas k))
                                                                                 k
                                                                                 (m/optional? k (schema/malli-form schema schemas))
                                                                                 map-widget-table)])
                               (rebuild! ctx))}}]))
    (widget-group/pack! window)
    window))

(defn- horiz-sep [colspan]
  (fn []
    [{:actor {:actor/type :actor.type/separator-horizontal}
      :pad-top 2
      :pad-bottom 2
      :colspan colspan
      :fill-x? true
      :expand-x? true}]))

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defmethod scene2d/build :actor.type/map-widget-table
  [{:keys [schema
           k->widget
           k->optional?
           ks-sorted
           opt?]}]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}
                :actor/name "cdq.db.schema.map.ui.widget"})
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
                                        (stage/add! stage (add-component-window (:db/schemas db) schema table)))}
                  :colspan colspan}])]
             [(when opt?
                [{:actor {:actor/type :actor.type/separator-horizontal}
                  :pad-top 2
                  :pad-bottom 2
                  :colspan colspan
                  :fill-x? true
                  :expand-x? true}])]
             component-rows))
    table))
