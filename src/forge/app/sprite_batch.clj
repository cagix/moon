(ns forge.app.sprite-batch
  (:require [forge.core :refer [bind-root
                                dispose
                                batch]])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn create [_]
  (bind-root batch (SpriteBatch.)))

(defn destroy [_]
  (dispose batch))
