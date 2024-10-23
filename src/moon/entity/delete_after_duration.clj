(ns ^:no-doc moon.entity.delete-after-duration
  (:require [moon.component :refer [defc]]
            [moon.info :as info]
            [gdl.utils :refer [readable-number]]
            [moon.world :refer [timer stopped? finished-ratio]]
            [moon.entity :as entity]))

(defc :entity/delete-after-duration
  {:let counter}
  (entity/->v [[_ duration]]
    (timer duration))

  (info/text [_]
    (str "[LIGHT_GRAY]Remaining: " (readable-number (finished-ratio counter)) "/1[]"))

  (entity/tick [_ eid]
    (when (stopped? counter)
      [[:e/destroy eid]])))

