(ns forge.app.sprite-batch
  (:require [forge.system :as system :refer [defmethods bind-root]])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defmethods :app/sprite-batch
  (system/create [_]
    (bind-root #'system/batch (SpriteBatch.)))
  (system/dispose [_]
    (.dispose system/batch)))
