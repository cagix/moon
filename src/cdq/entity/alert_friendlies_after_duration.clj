(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.timer :as timer]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/alert-friendlies-after-duration
  (entity/tick! [[_ {:keys [counter faction]}]
                 eid
                 {:keys [ctx/elapsed-time] :as ctx}]
    (when (timer/stopped? elapsed-time counter)
      (cons [:tx/mark-destroyed eid]
            (for [friendly-eid (->> {:position (entity/position @eid)
                                     :radius 4}
                                    (g/circle->entities ctx)
                                    (filter #(= (entity/faction @%) faction)))]
              [:tx/event friendly-eid :alert])))))
