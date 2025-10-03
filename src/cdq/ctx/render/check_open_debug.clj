(ns cdq.ctx.render.check-open-debug
  (:require [cdq.input :as input]
            [cdq.ui :as ui]))

(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)
