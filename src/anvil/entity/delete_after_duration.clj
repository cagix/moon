(ns ^:no-doc anvil.entity.delete-after-duration
  (:require [gdl.info :as info]
            [cdq.context :refer [timer finished-ratio stopped?]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]))

(defcomponent :entity/delete-after-duration
  (component/create [[_ duration] c]
    (timer c duration))

  (info/segment [counter c]
    (str "Remaining: " (readable-number (finished-ratio c counter)) "/1"))

  (component/tick [[_ counter] eid c]
    (when (stopped? c counter)
      (swap! eid assoc :entity/destroyed? true))))
