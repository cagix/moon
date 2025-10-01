(ns cdq.entity.animation.draw
  (:require [cdq.entity.animation :as animation]
            [cdq.entity.image.draw]))

(defn txs [animation entity ctx]
  (cdq.entity.image.draw/txs (animation/current-frame animation)
                             entity
                             ctx))
