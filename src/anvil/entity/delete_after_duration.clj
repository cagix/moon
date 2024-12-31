(ns ^:no-doc anvil.entity.delete-after-duration
  (:require [anvil.component :as component]
            [cdq.context :refer [timer finished-ratio stopped?]]
            [clojure.utils :refer [readable-number]]))

(defmethods :entity/delete-after-duration
  (component/->v [[_ duration] c]
    (timer c duration))

  (component/info [counter c]
    (str "Remaining: " (readable-number (finished-ratio c counter)) "/1"))

  (component/tick [[_ counter] eid c]
    (when (stopped? c counter)
      (swap! eid assoc :entity/destroyed? true))))
