(ns cdq.tx.rebuild-editor-window
  (:require [cdq.schema :as schema]
            cdq.schema.map
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]))

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
        property (cdq.schema.map/value nil map-widget-table (:schemas db))] ; FIXME
    (actor/remove! window)
    [[:tx/open-property-editor property]]))
