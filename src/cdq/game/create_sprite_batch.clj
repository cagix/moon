(ns cdq.game.create-sprite-batch
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn do! []
  (utils/bind-root #'ctx/batch (SpriteBatch.)))
