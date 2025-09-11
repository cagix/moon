(ns cdq.editor-window
  (:require [cdq.editor :as editor]
            [cdq.editor.window]
            [cdq.property :as property]
            [cdq.schema :as schema]
            [clojure.gdx.input :as input]))

(defn property-editor-window
  [{:keys [state
           schemas
           viewport-height]}
   ctx ; this also remove, pass widget
   property]
  (let [schema (get schemas (property/type property))
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
             :scrollpane-height viewport-height
             :widget widget}))))
