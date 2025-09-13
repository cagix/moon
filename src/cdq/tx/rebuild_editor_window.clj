(ns cdq.tx.rebuild-editor-window
  (:require [cdq.schema :as schema]
            cdq.schema.map
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
                             :scroll-pane
                             (group/find-actor "scroll-pane-table")
                             (group/find-actor "cdq.schema.map.ui.widget"))
        property (cdq.schema.map/value nil map-widget-table (:schemas db))] ; FIXME
    (actor/remove! window)
    [[:tx/open-property-editor property]]))
