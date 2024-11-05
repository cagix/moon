(ns moon.entity.animation
  (:require [gdl.animation :as animation]
            [gdl.system :refer [*k*]]
            [moon.world.time :as time]))

(defn- tx-assoc-image-current-frame [eid animation]
  [:e/assoc eid :entity/image (animation/current-frame animation)])

(defn create [animation eid]
  [(tx-assoc-image-current-frame eid animation)])

(defn tick [animation eid]
  [(tx-assoc-image-current-frame eid animation)
   [:e/assoc eid *k* (animation/tick animation time/delta)]])
