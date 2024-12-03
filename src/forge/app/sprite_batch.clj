(ns ^:no-doc forge.app.sprite-batch
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defmethods :app/sprite-batch
  (app-create [_]
    (bind-root #'batch (SpriteBatch.)))
  (app-dispose [_]
    (.dispose batch)))
