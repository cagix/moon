(ns cdq.scene2d.build.editor-window
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.db.schema :as schema]
            [cdq.input :as input]
            [gdl.throwable :as throwable]
            [cdq.ui :as ui]
            [cdq.ui.widget :as widget]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.window :as window]))

(defn- with-window-close [f]
  (fn [actor {:keys [ctx/stage]
              :as ctx}]
    (try
     (let [new-ctx (update ctx :ctx/db f)
           stage (actor/get-stage actor)]
       (stage/set-ctx! stage new-ctx))
     (actor/remove! (window/find-ancestor actor))
     (catch Throwable t
       (throwable/pretty-pst t)
       (ui/show-error-window! stage t)))))

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
                 (when (input/enter-just-pressed? input)
                   (clicked-save-fn actor ctx)))
        actors [{:actor/type :actor.type/actor
                 :actor/act act-fn}]
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
        actor (create* {:scroll-pane-height (ui/viewport-height stage)
                        :widget widget
                        :get-widget-value #(schema/value schema widget schemas)
                        :property-id (:property/id property)})]
    (scene2d/build actor)))
