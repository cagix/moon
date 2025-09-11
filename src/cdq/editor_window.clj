(ns cdq.editor-window
  (:require [cdq.db :as db]
            [cdq.property :as property]
            [cdq.editor.window :as editor.window]
            [cdq.stage]
            [cdq.schema :as schema]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn property-editor-window
  [{:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}
   property]
  (let [schema (get (:schemas db) (property/type property))
        widget (schema/create schema nil property ctx)]
    (editor.window/create
     {:save-fn (fn [{:keys [ctx/db]}]
                 (swap! application-state update :ctx/db
                        db/update!
                        (schema/value schema nil widget (:schemas db))))
      :delete-fn (fn [_ctx]
                   (swap! application-state update :ctx/db
                          db/delete!
                          (:property/id property)))
      :scrollpane-height (cdq.stage/viewport-height stage)
      :widget widget})))

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
