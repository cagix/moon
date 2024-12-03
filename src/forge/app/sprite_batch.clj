(ns forge.app.sprite-batch
  (:require [forge.context :as context]
            [forge.system :refer [defmethods bind-root]]
            [forge.lifecycle :as lifecycle])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defmethods :app/sprite-batch
  (lifecycle/create [_]
    (bind-root #'context/batch (SpriteBatch.)))
  (lifecycle/dispose [_]
    (.dispose context/batch)))
