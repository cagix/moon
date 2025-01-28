(ns cdq.create.shape-drawer-texture
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            clojure.gdx.utils))

(defn create []
  (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color color/white)
                 (pixmap/draw-pixel 0 0))
        texture (texture/create pixmap)]
    (clojure.gdx.utils/dispose pixmap)
    texture))
