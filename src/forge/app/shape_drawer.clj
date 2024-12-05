(ns forge.app.shape-drawer
  (:require [forge.core :refer [bind-root
                                dispose
                                batch
                                shape-drawer]])
  (:import (com.badlogic.gdx.graphics Color Texture Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(declare ^:private pixel-texture)

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(defn create []
  (bind-root #'pixel-texture (white-pixel-texture))
  (bind-root #'shape-drawer (ShapeDrawer. batch
                                          (TextureRegion. ^Texture pixel-texture 1 0 1 1))))

(defn destroy []
  (dispose pixel-texture))
