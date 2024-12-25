(ns ^:no-doc anvil.entity.delete-after-duration
  (:require [anvil.component :as component]
            [anvil.world :refer [timer finished-ratio stopped?]]))

(defmethods :entity/delete-after-duration
  (component/->v  [[_ duration]]
    (timer duration))

  (component/info [counter]
    (str "Remaining: " (readable-number (finished-ratio counter)) "/1"))

  (component/tick [[_ counter] eid c]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true))))
