(ns forge.entity.delete-after-animation-stopped?
  (:require [clojure.utils :refer [defmethods]]
            [forge.animation :as animation]
            [forge.entity :refer [create tick]]))

(defmethods :entity/delete-after-animation-stopped
  (create  [_ eid]
    (-> @eid :entity/animation :looping? not assert))

  (tick [_ eid]
    (when (animation/stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))
