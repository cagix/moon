(ns cdq.game.update-stage
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]))

(defn do! []
  (stage/act! ctx/stage))
