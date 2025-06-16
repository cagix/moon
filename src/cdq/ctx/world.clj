(ns cdq.ctx.world
  (:require [cdq.ctx :as ctx]))

(defn creatures-in-los-of-player [{:keys [ctx/world
                                          ctx/player-eid]
                                   :as ctx}]
  (->> (:world/active-entities world)
       (filter #(:entity/species @%))
       (filter #(ctx/line-of-sight? ctx @player-eid @%))
       (remove #(:entity/player? @%))))
