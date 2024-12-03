(ns ^:no-doc forge.app.shape-drawer
  (:require [forge.core :refer :all])
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
  (app-create [_]
    (bind-root #'pixel-texture (white-pixel-texture))
    (bind-root #'shape-drawer (ShapeDrawer. batch (TextureRegion. pixel-texture 1 0 1 1))))
  (app-dispose [_]
    (.dispose pixel-texture)))
