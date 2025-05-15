(ns cdq.game.shape-drawer
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer (graphics/shape-drawer ctx/batch
                                                             (TextureRegion. ^Texture ctx/shape-drawer-texture 1 0 1 1))))
