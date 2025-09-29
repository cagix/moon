(ns cdq.ui.editor.window
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.malli :as m]
            [cdq.db.schema :as schema]
            [cdq.db.schemas :as schemas]
            [cdq.stage]
            [cdq.ui.editor.value-widget :as value-widget]
            [cdq.ui.editor.map-widget-table :as map-widget-table]
            [cdq.ui.widget :as widget]
            [clojure.input :as input]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.group :as group]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.table :as table]
            [gdl.scene2d.ui.window :as window]
            [gdl.scene2d.ui.widget-group :as widget-group]))

(defn do! [ctx] ctx)

(defn- with-window-close [f]
  (fn [actor ctx]
    (try
     (let [new-ctx (update ctx :ctx/db f)
           stage (actor/get-stage actor)]
       (stage/set-ctx! stage new-ctx))
     (actor/remove! (window/find-ancestor actor))
     (catch Throwable t
       (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                             [:tx/show-error-window t]])))))

(defn- update-property-fn [get-widget-value]
  (fn [db]
    (db/update! db (get-widget-value))))

(defn- delete-property-fn [property-id]
  (fn [db]
    (db/delete! db property-id)))

(defn- create*
  [{:keys [scroll-pane-height
           widget
           get-widget-value
           property-id]}]
  (let [clicked-delete-fn (with-window-close (delete-property-fn property-id))
        clicked-save-fn   (with-window-close (update-property-fn get-widget-value))
        act-fn (fn [actor _delta {:keys [ctx/input] :as ctx}]
                 (when (input/key-just-pressed? input :enter)
                   (clicked-save-fn actor ctx)))
        actors [{:actor/type :actor.type/actor
                 :act act-fn}]
        save-button {:actor/type :actor.type/text-button
                     :text "Save [LIGHT_GRAY](ENTER)[]"
                     :on-clicked clicked-save-fn}
        delete-button {:actor/type :actor.type/text-button
                       :text "Delete"
                       :on-clicked clicked-delete-fn}
        scroll-pane-rows [[{:actor widget :colspan 2}]
                          [{:actor save-button :center? true}
                           {:actor delete-button :center? true}]]
        rows [[(widget/scroll-pane-cell scroll-pane-height
                                        scroll-pane-rows)]]]
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
     :pack? true}))

(defmethod scene2d/build :actor.type/editor-window
  [{:keys [ctx
           property]}]
  (let [{:keys [ctx/db
                ctx/stage]} ctx
        schemas (:db/schemas db)
        schema (get schemas (property/type property))
        ; build for get-widget-value
        ; or find a way to find the widget from the context @ save button
        ; should be possible
        widget (scene2d/build (schema/create schema property ctx))
        actor (create* {:scroll-pane-height (cdq.stage/viewport-height stage)
                        :widget widget
                        :get-widget-value #(schema/value schema widget schemas)
                        :property-id (:property/id property)})]
    (scene2d/build actor)))

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
        remaining-ks (sort (remove (set (keys (schema/value schema map-widget-table schemas)))
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

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  cdq.ui.editor.map-widget-table/MapWidgetTable
  (get-value [table schemas]
    (into {}
          (for [widget (filter (comp vector? actor/user-object) (group/children table))
                :let [[k _] (actor/user-object widget)]]
            [k (schema/value (get schemas k) widget schemas)]))))
