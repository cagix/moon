(ns cdq.game.assoc-active-entities
  (:require [cdq.ctx.world :as world]))

(defn do! [ctx]
  (update ctx :ctx/world world/cache-active-entities @(:ctx/player-eid ctx)))
