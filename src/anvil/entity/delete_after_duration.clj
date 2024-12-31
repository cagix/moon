(ns ^:no-doc anvil.entity.delete-after-duration
  (:require [anvil.entity :as entity]
            [gdl.info :as info]
            [cdq.context :refer [timer finished-ratio stopped?]]
            [clojure.utils :refer [defmethods readable-number]]))

(defmethods :entity/delete-after-duration
  (entity/->v [[_ duration] c]
    (timer c duration))

  (info/segment [counter c]
    (str "Remaining: " (readable-number (finished-ratio c counter)) "/1"))

  (entity/tick [[_ counter] eid c]
    (when (stopped? c counter)
      (swap! eid assoc :entity/destroyed? true))))
