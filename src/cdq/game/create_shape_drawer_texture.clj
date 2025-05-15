(ns cdq.game.create-shape-drawer-texture
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture)))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer-texture (white-pixel-texture)))
