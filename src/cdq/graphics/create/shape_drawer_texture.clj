(ns cdq.graphics.create.shape-drawer-texture
  (:require [clojure.graphics.color :as color]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [com.badlogic.gdx.graphics.texture :as texture]))

(defn create
  [graphics]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (pixmap/create 1 1 :pixmap.format/RGBA8888)
                                                                (pixmap/set-color! color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (texture/create pixmap)]
                                                   (pixmap/dispose! pixmap)
                                                   texture)))
