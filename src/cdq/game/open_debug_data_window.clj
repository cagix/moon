(ns cdq.game.open-debug-data-window
  (:require [cdq.dev.data-view :as data-view]
            [cdq.ui.stage :as stage]
            [cdq.world.grid :as grid]))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)

(defn do!
  [{:keys [ctx/stage
           ctx/mouseover-eid
           ctx/world
           ctx/world-mouse-position]}]
  (let [data (or (and mouseover-eid @mouseover-eid)
                 @(grid/cell (:world/grid world)
                             (mapv int world-mouse-position)))]
    (stage/add! stage
                (data-view/table-view-window {:title "Data View"
                                              :data data
                                              :width 500
                                              :height 500}))))
