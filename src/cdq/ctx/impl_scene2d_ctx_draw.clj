(ns cdq.ctx.impl-scene2d-ctx-draw
  (:require [cdq.graphics :as graphics]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]))

(defn do! [ctx]
  (extend-type (class ctx)
    ctx/Graphics
    (draw! [{:keys [ctx/graphics]} draws]
      (graphics/handle-draws! graphics draws)))
  ctx)
