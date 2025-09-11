(ns cdq.tx.rebuild-editor-window
  (:require [cdq.editor-window]
            [cdq.schema :as schema]
            [cdq.stage]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (:property-editor-window stage)
        map-widget-table (-> window
                             :scroll-pane
                             (group/find-actor "scroll-pane-table")
                             :map-widget)
        prop-value (schema/value [:s/map] nil map-widget-table (:schemas db))]
    (actor/remove! window)
    (stage/add! stage (actor/build (cdq.editor-window/property-editor-window
                                    {:state application-state
                                     :schemas (:schemas db)
                                     :viewport-height (cdq.stage/viewport-height stage)}
                                    ctx
                                    prop-value)))))
