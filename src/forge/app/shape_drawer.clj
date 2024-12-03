(ns ^:no-doc forge.app.shape-drawer
  (:require [forge.system :as system])
  (:import (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(declare ^:private ^Texture pixel-texture)

(defmethods :app/shape-drawer
  (system/create [_]
    (bind-root #'pixel-texture (white-pixel-texture))
    (bind-root #'system/shape-drawer (ShapeDrawer. system/batch (TextureRegion. pixel-texture 1 0 1 1))))
  (system/dispose [_]
    (.dispose pixel-texture)))
