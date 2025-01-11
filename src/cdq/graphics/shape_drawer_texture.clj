(ns cdq.graphics.shape-drawer-texture
  (:require [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [gdl.graphics.color :as color]
            [gdl.utils :refer [dispose]]))

(defn create [_context _config]
  (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color color/white)
                 (pixmap/draw-pixel 0 0))
        texture (texture/create pixmap)]
    (dispose pixmap)
    texture))
