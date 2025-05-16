(ns cdq.game.update-potential-fields
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! []
  (world/update-potential-fields! ctx/world))
