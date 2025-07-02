(ns cdq.render.debug-data-view
  (:require [cdq.dev.data-view :as data-view]
            [cdq.grid :as grid]
            [clojure.input :as input]
            [gdl.c]
            [gdl.ui.stage :as stage]))

(defn do!
  [{:keys [ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [mouseover-eid (:world/mouseover-eid world)
          data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int (gdl.c/world-mouse-position ctx))))]
      (stage/add! stage (data-view/table-view-window {:title "Data View"
                                                      :data data
                                                      :width 500
                                                      :height 500}))))
  ctx)
