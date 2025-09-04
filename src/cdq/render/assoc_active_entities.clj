(ns cdq.render.assoc-active-entities
  (:require [cdq.world.content-grid :as content-grid]))

(defn do!
  [{:keys [ctx/content-grid
           ctx/player-eid]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/active-entities]
            (content-grid/active-entities content-grid @player-eid)))
