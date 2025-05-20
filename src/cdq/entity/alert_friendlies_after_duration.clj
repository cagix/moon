(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/alert-friendlies-after-duration
  (entity/tick! [[_ {:keys [counter faction]}]
                 eid
                 {:keys [ctx/elapsed-time
                         ctx/grid]}]
    (when (timer/stopped? elapsed-time counter)
      (cons [:tx/mark-destroyed eid]
            (for [friendly-eid (->> {:position (:position @eid)
                                     :radius 4}
                                    (grid/circle->entities grid)
                                    (filter #(= (:entity/faction @%) faction)))]
              [:tx/event friendly-eid :alert])))))
