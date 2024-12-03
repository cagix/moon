(ns forge.app.shape-drawer
  (:require [forge.context :as context]
            [forge.system :refer [defmethods bind-root]]
            [forge.lifecycle :as lifecycle])
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

(defmethods :app/shape-drawer
  (lifecycle/create [_]
    (def ^:private ^Texture pixel-texture (white-pixel-texture))
    (bind-root #'context/shape-drawer (ShapeDrawer. context/batch (TextureRegion. pixel-texture 1 0 1 1))))
  (lifecycle/dispose [_]
    (.dispose pixel-texture)))
