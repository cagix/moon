(ns anvil.entity.delete-after-duration
  (:require [anvil.component :as component]
            [anvil.world :refer [timer finished-ratio stopped?]]
            [gdl.utils :refer [defmethods index-of readable-number]]))

(defmethods :entity/delete-after-duration
  (component/->v  [[_ duration]]
    (timer duration))

  (component/info [counter]
    (str "Remaining: " (readable-number (finished-ratio counter)) "/1"))

  (component/tick [[_ counter] eid]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true))))
