(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.entity :as entity]
            [cdq.world.grid :as grid]
            [cdq.timer :as timer]))

(defn tick!
  [{:keys [counter faction]}
   eid
   {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    (cons [:tx/mark-destroyed eid]
          (for [friendly-eid (->> {:position (entity/position @eid)
                                   :radius 4}
                                  (grid/circle->entities (:world/grid world))
                                  (filter #(= (:entity/faction @%) faction)))]
            [:tx/event friendly-eid :alert]))))
