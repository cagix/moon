(ns cdq.render.assoc-active-entities
  (:require [cdq.world.content-grid :as content-grid]))

(defn do!
  [{:keys [ctx/player-eid
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/active-entities]
            (content-grid/active-entities (:world/content-grid world)
                                          @player-eid)))
