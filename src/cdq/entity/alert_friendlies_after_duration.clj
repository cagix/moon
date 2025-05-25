(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/alert-friendlies-after-duration
  (entity/tick! [[_ {:keys [counter faction]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      (cons [:tx/mark-destroyed eid]
            (for [friendly-eid (->> {:position (entity/position @eid)
                                     :radius 4}
                                    (g/circle->entities ctx)
                                    (filter #(= (entity/faction @%) faction)))]
              [:tx/event friendly-eid :alert])))))
