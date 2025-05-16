(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/alert-friendlies-after-duration
  (entity/tick! [[_ {:keys [counter faction]}] eid]
    (when (timer/stopped? ctx/elapsed-time counter)
      (cons [:tx/mark-destroyed eid]
            (for [friendly-eid (->> {:position (:position @eid)
                                     :radius 4}
                                    (grid/circle->entities ctx/grid)
                                    (filter #(= (:entity/faction @%) faction)))]
              [:tx/event friendly-eid :alert])))))
