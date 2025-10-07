(ns cdq.graphics.create.batch
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn create
  [graphics]
  (assoc graphics :graphics/batch (SpriteBatch.)))
