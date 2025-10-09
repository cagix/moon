(ns cdq.scene2d.build.map-widget-table
  (:require [malli.utils :as mu]
            [cdq.ui.editor.schema]
            [cdq.db.schema :as schema]
            [cdq.db.schemas :as schemas]
            [cdq.ui.editor.value-widget :as value-widget]
            [cdq.ui.editor.map-widget-table :as map-widget-table]
            [cdq.ui.editor.window :as editor-window]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.ui.table :as table]
            [clojure.scene2d.vis-ui.window :as window]
            [clojure.vis-ui.label :as label])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn- rebuild!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (-> stage
                   .getRoot
                   (Group/.findActor "cdq.ui.editor.window"))
        map-widget-table (-> window
                             (Group/.findActor "cdq.ui.widget.scroll-pane-table")
                             (Group/.findActor "scroll-pane-table")
                             (Group/.findActor "cdq.db.schema.map.ui.widget"))
        property (map-widget-table/get-value map-widget-table (:db/schemas db))]
    (Actor/.remove window)
    (.addActor stage
               (editor-window/create
                {:ctx ctx
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
                                             (Actor/.remove (first (filter (fn [actor]
                                                                             (and (Actor/.getUserObject actor)
                                                                                  (= k ((Actor/.getUserObject actor) 0))))
                                                                           (Group/.getChildren table))))
                                             (rebuild! ctx))})
                     :left? true}
                    {:actor (label/create label-text)}]]}
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
  (let [window (window/create {:title "Choose"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (cdq.ui.editor.schema/value schema map-widget-table schemas)))
                                   (mu/map-keys (schema/malli-form schema schemas))))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [{:actor {:actor/type :actor.type/text-button
                 :text (name k)
                 :on-clicked (fn [_actor ctx]
                               (Actor/.remove window)
                               (table/add-rows! map-widget-table [(component-row (value-widget/build ctx
                                                                                                     (get schemas k)
                                                                                                     k
                                                                                                     (schemas/default-value schemas k))
                                                                                 k
                                                                                 (mu/optional? k (schema/malli-form schema schemas))
                                                                                 map-widget-table)])
                               (rebuild! ctx))}}]))
    (.pack window)
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
                                        (.addActor stage (add-component-window (:db/schemas db) schema table)))}
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
