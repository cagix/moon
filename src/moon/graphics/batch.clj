(ns moon.graphics.batch
  (:require [gdl.utils :as utils])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(declare batch)

(defn init []
  (bind-root #'batch (SpriteBatch.)))

(defn dispose []
  (utils/dispose batch))
