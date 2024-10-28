(ns moon.entity.delete-after-duration
  (:require [gdl.utils :refer [readable-number]]
            [moon.component :refer [defc] :as component]
            [moon.entity :as entity]
            [moon.world.time :refer [timer stopped? finished-ratio]]))

(defc :entity/delete-after-duration
  {:let counter}
  (entity/->v [[_ duration]]
    (timer duration))

  (component/info [_]
    (str "[LIGHT_GRAY]Remaining: " (readable-number (finished-ratio counter)) "/1[]"))

  (entity/tick [_ eid]
    (when (stopped? counter)
      [[:e/destroy eid]])))

