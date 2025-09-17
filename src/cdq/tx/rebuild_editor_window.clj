(ns cdq.tx.rebuild-editor-window
  (:require [cdq.ui.editor.map-widget-table :as map-widget-table]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (-> stage
                   stage/root
                   (group/find-actor "cdq.ui.editor.window"))
        map-widget-table (-> window
                             (group/find-actor "cdq.ui.widget.scroll-pane-table")
                             (group/find-actor "scroll-pane-table")
                             (group/find-actor "cdq.schema.map.ui.widget"))
        property (map-widget-table/get-value map-widget-table (:schemas db))]
    (actor/remove! window)
    [[:tx/open-property-editor property]]))
