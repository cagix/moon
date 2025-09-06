(ns cdq.entity.animation
  (:require [cdq.animation :as animation]))

(defn create [v _ctx]
  (animation/create v))
