(ns cdq.ctx.world
  (:require [cdq.ctx :as ctx]))

(defn creatures-in-los-of-player [{:keys [ctx/active-entities
                                          ctx/player-eid]
                                   :as ctx}]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(ctx/line-of-sight? ctx @player-eid @%))
       (remove #(:entity/player? @%))))
