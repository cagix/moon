(ns cdq.game.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! []
  (world/remove-destroyed-entities! ctx/world))
