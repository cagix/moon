(ns moon.entity.animation
  (:require [gdl.animation :as animation]
            [moon.world.time :as time]))

(defn- tx-assoc-image-current-frame [eid animation]
  [:e/assoc eid :entity/image (animation/current-frame animation)])

(defn create [[_ animation] eid]
  [(tx-assoc-image-current-frame eid animation)])

(defn tick [[k animation] eid]
  [(tx-assoc-image-current-frame eid animation)
   [:e/assoc eid k (animation/tick animation time/delta)]])
