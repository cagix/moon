(ns cdq.game.assoc-active-entities
  (:require [cdq.world.content-grid :as content-grid]))

(defn- cache-active-entities [world entity]
  (assoc world :world/active-entities
         (content-grid/active-entities (:world/content-grid world)
                                       entity)))

(defn do! [ctx]
  (update ctx :ctx/world cache-active-entities @(:ctx/player-eid ctx)))
