(ns cdq.ctx.check-open-debug
  (:require [cdq.input :as input]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (stage/add! stage (scene2d/build
                         {:actor/type :actor.type/data-viewer
                          :title "Data View"
                          :data data
                          :width 500
                          :height 500}))))
  ctx)
