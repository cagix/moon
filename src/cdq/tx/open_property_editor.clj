(ns cdq.tx.open-property-editor
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.stage]
            [cdq.property :as property]
            [cdq.ui.widget :as widget]
            [gdl.input :as input]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.window :as window]))

(defn- with-window-close [f]
  (fn [actor ctx]
    (try (f)
         (actor/remove! (window/find-ancestor actor))
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))))

(defn- update-property-fn [get-widget-value]
  (fn []
    (swap! application/state update :ctx/db db/update! (get-widget-value))))

(defn- delete-property-fn [property-id]
  (fn []
    (swap! application/state update :ctx/db db/delete! property-id)))

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

(defn do!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}
   property]
  (let [schemas (:schemas db)
        schema (get schemas (property/type property))
        ; build for get-widget-value
        ; or find a way to find the widget from the context @ save button
        ; should be possible
        widget (scene2d/build (schema/create schema property ctx))
        actor (create* {:scroll-pane-height (cdq.stage/viewport-height stage)
                        :widget widget
                        :get-widget-value #(schema/value schema widget schemas)
                        :property-id (:property/id property)})]
    (stage/add! stage (scene2d/build actor)))
  nil)
