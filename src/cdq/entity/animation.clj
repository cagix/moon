(ns cdq.entity.animation
  (:require [cdq.animation :as animation]))

(defn create [v _ctx]
  (animation/create v))

(defn tick! [animation eid {:keys [ctx/delta-time]}]
  [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]])
