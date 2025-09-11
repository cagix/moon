(ns cdq.editor-window
  (:require [cdq.editor :as editor]
            [cdq.editor.window]
            [cdq.property :as property]
            [cdq.stage]
            [cdq.schema :as schema]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn property-editor-window
  [{:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}
   property]
  (let [state application-state
        ui-viewport-height (cdq.stage/viewport-height stage)
        schemas (:schemas db)
        schema (get schemas (property/type property))
        widget (schema/create schema nil property ctx)
        get-widget-value #(schema/value schema nil widget schemas)
        property-id (:property/id property)
        {:keys [clicked-save-fn]
         :as button-handlers} (editor/create-button-handlers state
                                                             property-id
                                                             get-widget-value)
        act-fn (fn [actor _delta {:keys [ctx/input] :as ctx}]
                 (when (input/key-just-pressed? input :enter)
                   (clicked-save-fn actor ctx)))]
    (cdq.editor.window/create
     (merge button-handlers
            {:act-fn act-fn
             :scrollpane-height ui-viewport-height
             :widget widget}))))

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
