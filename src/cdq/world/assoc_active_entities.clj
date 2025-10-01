(ns cdq.world.assoc-active-entities
  (:require [cdq.world.content-grid :as content-grid]))

(defn do!
  [{:keys [world/content-grid
           world/player-eid]
    :as world}]
  (assoc world
         :world/active-entities
         (content-grid/active-entities content-grid
                                       @player-eid)))
