(ns cdq.editor-window
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.property :as property]
            [cdq.stage]
            [cdq.schema :as schema]
            [cdq.ui.widget]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.window :as window]))

(defn- update-property-fn [state get-widget-value]
  (fn [_ctx]
    (swap! state update :ctx/db db/update! (get-widget-value))))

(defn- delete-property-fn [state property-id]
  (fn [_ctx]
    (swap! state update :ctx/db db/delete! property-id)))

(defn- with-window-close [f]
  (fn [actor ctx]
    (try (f ctx)
         (actor/remove! (window/find-ancestor actor))
         (catch Throwable t
           ; TODO this i can move one up in each of my actors 'act' fns ...
           ; so the game keeps running ...
           ; just skip one render/etc ?
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))))

(defn- create-base
  [{:keys [scrollpane-height
           widget
           clicked-save-fn
           clicked-delete-fn
           act-fn]}]
  {:actor/type :actor.type/window
   :title "[SKY]Property[]"
   :id :property-editor-window
   :modal? true
   :close-button? true
   :center? true
   :close-on-escape? true
   :rows [[(cdq.ui.widget/scroll-pane-cell
            scrollpane-height
            [[{:actor widget :colspan 2}]
             [{:actor {:actor/type :actor.type/text-button
                       :text "Save [LIGHT_GRAY](ENTER)[]"
                       :on-clicked clicked-save-fn}
               :center? true}
              {:actor {:actor/type :actor.type/text-button
                       :text "Delete"
                       :on-clicked clicked-delete-fn}
               :center? true}]])]]
   :actors [{:actor/type :actor.type/actor
             :act act-fn}]
   :cell-defaults {:pad 5}
   :pack? true})

(defn- create-config
  [property
   {:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}]
  (let [schema (get (:schemas db) (property/type property))
        widget (schema/create schema nil property ctx)
        property-id (:property/id property)
        get-widget-value #(schema/value schema nil widget (:schemas db))
        clicked-save-fn (with-window-close (update-property-fn application-state get-widget-value))]
    {:scrollpane-height (cdq.stage/viewport-height stage)
     :widget widget
     :clicked-save-fn   clicked-save-fn
     :clicked-delete-fn (with-window-close (delete-property-fn application-state property-id))
     :act-fn (fn [actor _delta {:keys [ctx/input] :as ctx}]
               (when (input/key-just-pressed? input :enter)
                 (clicked-save-fn actor ctx)))}))

(defn property-editor-window
  [ctx property]
  (->> ctx
       (create-config property)
       create-base))

(defn rebuild!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (:property-editor-window stage)
        map-widget-table (-> window
                             :scroll-pane
                             (group/find-actor "scroll-pane-table")
                             :map-widget)
        prop-value (schema/value [:s/map] nil map-widget-table (:schemas db))]
    (actor/remove! window)
    (stage/add! stage (actor/build (property-editor-window ctx prop-value)))))
