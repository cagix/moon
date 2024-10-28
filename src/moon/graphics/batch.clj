(ns moon.graphics.batch
  (:require [gdl.utils :refer [dispose]]
            [moon.app :as app]
            [moon.component :refer [defc]])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(declare batch)

(defc :moon.graphics.batch
  (app/create [_]
    (bind-root #'batch (SpriteBatch.)))
  (app/dispose [_]
    (dispose batch)))
