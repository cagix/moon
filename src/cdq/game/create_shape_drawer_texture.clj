(ns cdq.game.create-shape-drawer-texture
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn- white-pixel-texture-region []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    (TextureRegion. texture 1 0 1 1)))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer-texture (white-pixel-texture-region)))
