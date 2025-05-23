(ns cdq.entity.delete-after-duration
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration] ctx]
    (g/create-timer ctx duration))

  (entity/tick! [[_ counter] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/mark-destroyed eid]])))
