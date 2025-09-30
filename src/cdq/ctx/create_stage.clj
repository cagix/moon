(ns cdq.ctx.create-stage
  (:require [cdq.graphics :as graphics]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}]
  (extend-type (class ctx)
    ctx/Graphics
    (draw! [{:keys [ctx/graphics]} draws]
      (graphics/handle-draws! graphics draws)))
  (assoc ctx :ctx/stage (stage/create (:graphics/ui-viewport graphics)
                                      (:graphics/batch       graphics))))
