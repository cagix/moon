(ns cdq.graphics.create.shape-drawer-texture
  (:import (com.badlogic.gdx.graphics Color
                                      Texture
                                      Pixmap
                                      Pixmap$Format)))

(defn create
  [graphics]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                                                (.setColor Color/WHITE)
                                                                (.drawPixel 0 0))
                                                       texture (Texture. pixmap)]
                                                   (.dispose pixmap)
                                                   texture)))
