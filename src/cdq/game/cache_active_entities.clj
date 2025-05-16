(ns cdq.game.cache-active-entities
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [cdq.world.content-grid :as content-grid]))

(defn do! []
  (bind-root #'ctx/active-entities
             (content-grid/active-entities ctx/content-grid
                                           @ctx/player-eid)))
