(ns moon.graphics.batch
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn init []
  (def batch (SpriteBatch.)))

(defn dispose []
  (.dispose batch))
