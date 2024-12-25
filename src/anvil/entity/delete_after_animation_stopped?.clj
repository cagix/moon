(ns ^:no-doc anvil.entity.delete-after-animation-stopped?
  (:require [anvil.component :as component]
            [gdl.graphics.animation :as animation]))

(defmethods :entity/delete-after-animation-stopped?
  (component/create [_ eid]
    (-> @eid :entity/animation :looping? not assert))

  (component/tick [_ eid c]
    (when (animation/stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))
