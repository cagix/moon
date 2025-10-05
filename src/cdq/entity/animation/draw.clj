(ns cdq.entity.animation.draw
  (:require [cdq.entity.image.draw]
            [clojure.animation :as animation]))

(defn txs [animation entity ctx]
  (cdq.entity.image.draw/txs (animation/current-frame animation)
                             entity
                             ctx))
