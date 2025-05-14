(ns cdq.game.draw-stage
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]))

(defn do! []
  (stage/draw! ctx/stage))
