(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.world.entity :as entity]
            [cdq.world.grid :as grid]
            [cdq.timer :as timer]))

(defn tick! [{:keys [counter faction]}
             eid
             {:keys [world/elapsed-time
                     world/grid]}]
  (when (timer/stopped? elapsed-time counter)
    (cons [:tx/mark-destroyed eid]
          (for [friendly-eid (->> {:position (entity/position @eid)
                                   :radius 4}
                                  (grid/circle->entities grid)
                                  (filter #(= (:entity/faction @%) faction)))]
            [:tx/event friendly-eid :alert]))))
