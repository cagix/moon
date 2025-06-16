(ns cdq.ctx.world
  (:require [cdq.w :as w]))

(defn creatures-in-los-of-player [{:keys [ctx/world]}]
  (let [player-eid (:world/player-eid world)]
    (->> (:world/active-entities world)
         (filter #(:entity/species @%))
         (filter #(w/line-of-sight? world @player-eid @%))
         (remove #(:entity/player? @%)))))
