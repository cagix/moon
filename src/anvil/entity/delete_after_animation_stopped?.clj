(ns ^:no-doc anvil.entity.delete-after-animation-stopped?
  (:require [anvil.component :as component]
            [gdl.graphics.animation :as animation]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/delete-after-animation-stopped?
  (component/create [_ eid]
    (-> @eid :entity/animation :looping? not assert))

  (component/tick [_ eid]
    (when (animation/stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))
