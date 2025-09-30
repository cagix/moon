(ns cdq.ctx.create-stage
  (:require [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}]
  (assoc ctx :ctx/stage (stage/create (:graphics/ui-viewport graphics)
                                      (:graphics/batch       graphics))))
