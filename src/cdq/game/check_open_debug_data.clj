(ns cdq.game.check-open-debug-data
  (:require [cdq.ctx.input :as input]
            [cdq.world.grid :as grid]
            [cdq.ui.stage]
            [cdq.dev.data-view :as data-view]))

(defn do!
  [{:keys [ctx/input
           ctx/stage
           ctx/mouseover-eid
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int world-mouse-position)))]
      (cdq.ui.stage/add! stage
                         (data-view/table-view-window {:title "Data View"
                                                       :data data
                                                       :width 500
                                                       :height 500}))))
  ctx)
