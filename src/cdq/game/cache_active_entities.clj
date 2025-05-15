(ns cdq.game.cache-active-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! []
  (alter-var-root #'ctx/world world/cache-active-entities))
