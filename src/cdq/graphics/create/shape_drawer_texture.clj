(ns cdq.graphics.create.shape-drawer-texture
  (:require [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.graphics.color :as color]))

(defn create
  [graphics]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (pixmap/create 1 1 :pixmap.format/RGBA8888)
                                                                (pixmap/set-color! color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (pixmap/texture pixmap)]
                                                   (pixmap/dispose! pixmap)
                                                   texture)))
