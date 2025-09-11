(ns cdq.tx.open-property-editor
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.editor.window :as editor-window]
            [cdq.schema :as schema]
            [cdq.stage]
            [cdq.property :as property]
            [cdq.ui.widget :as widget]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.window :as window]))

(defn- with-window-close [f]
  (fn [actor ctx]
    (try (f)
         (actor/remove! (window/find-ancestor actor))
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))))

(defn- update-property-fn [state get-widget-value]
  (fn []
    (swap! state update :ctx/db db/update! (get-widget-value))))

(defn- delete-property-fn [state property-id]
  (fn []
    (swap! state update :ctx/db db/delete! property-id)))

(defn do!
  [{:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}
   property]
  (let [scroll-pane-height (cdq.stage/viewport-height stage)
        schemas (:schemas db)
        schema (get schemas (property/type property))
        widget (schema/create schema nil property ctx)
        get-widget-value #(schema/value schema nil widget schemas)
        property-id (:property/id property)
        clicked-delete-fn (with-window-close (delete-property-fn application-state property-id))
        clicked-save-fn   (with-window-close (update-property-fn application-state get-widget-value))
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
                                        scroll-pane-rows)]]
        actor (editor-window/create {:rows rows
                                     :actors actors})]
    (stage/add! stage (actor/build actor)))
  nil)
