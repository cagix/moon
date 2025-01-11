(ns cdq.graphics.batch
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn create [context _config]
  (assoc context :gdl.graphics/batch (SpriteBatch.)))
