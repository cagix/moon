(ns ^:no-doc anvil.entity.delete-after-animation-stopped?
  (:require [anvil.entity :as entity]
            [clojure.utils :refer [defmethods]]
            [gdl.graphics.animation :as animation]))

(defmethods :entity/delete-after-animation-stopped?
  (entity/create [_ eid c]
    (-> @eid :entity/animation :looping? not assert))

  (entity/tick [_ eid c]
    (when (animation/stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))
