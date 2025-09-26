(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.timer :as timer]
            [cdq.world.grid :as grid]))

(defn tick
  [{:keys [counter faction]}
   eid
   {:keys [world/elapsed-time
           world/grid]}]
  (when (timer/stopped? elapsed-time counter)
    (cons [:tx/mark-destroyed eid]
          (for [friendly-eid (->> {:position (:body/position (:entity/body @eid))
                                   :radius 4}
                                  (grid/circle->entities grid)
                                  (filter #(= (:entity/faction @%) faction)))]
            [:tx/event friendly-eid :alert]))))
