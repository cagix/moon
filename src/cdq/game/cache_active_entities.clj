(ns cdq.game.cache-active-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world.content-grid :as content-grid]))

(defn do! []
  (alter-var-root #'ctx/world assoc :active-entities (content-grid/active-entities (:content-grid ctx/world) @ctx/player-eid)))
