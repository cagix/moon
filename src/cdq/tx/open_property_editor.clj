(ns cdq.tx.open-property-editor
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.ui.editor.window :as editor-window]
            [cdq.schema :as schema]
            [cdq.ctx.stage]
            [cdq.property :as property]
            [cdq.ui.widget :as widget]
            [clojure.gdx.input :as input]
            [clojure.scene2d.actor :as actor]
            [clojure.gdx.scene2d.actor.decl :as actor.decl]
            [clojure.scene2d.stage :as stage]
            [clojure.gdx.scene2d.ui.window :as window]))

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

(defn- create*
  [{:keys [state
           scroll-pane-height
           widget
           get-widget-value
           property-id]}]
  (let [clicked-delete-fn (with-window-close (delete-property-fn state property-id))
        clicked-save-fn   (with-window-close (update-property-fn state get-widget-value))
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
                                     :group/actors actors})]
    (editor-window/create {:rows rows
                           :group/actors actors})))

(defn do!
  [{:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}
   property]
  (let [schemas (:schemas db)
        schema (get schemas (property/type property))
        widget (schema/create schema property ctx)
        actor (create* {:state application-state
                        :scroll-pane-height (cdq.ctx.stage/viewport-height stage)
                        :widget widget
                        :get-widget-value #(schema/value schema widget schemas)
                        :property-id (:property/id property)})]
    (stage/add! stage (actor.decl/build actor)))
  nil)
