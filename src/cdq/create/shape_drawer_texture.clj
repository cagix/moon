(ns cdq.create.shape-drawer-texture
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.texture :as texture]
            clojure.gdx.utils)
  (:import (com.badlogic.gdx.graphics Pixmap Pixmap$Format)))

(defn create []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor color/white)
                 (.drawPixel 0 0))
        texture (texture/create pixmap)]
    (clojure.gdx.utils/dispose pixmap)
    texture))
