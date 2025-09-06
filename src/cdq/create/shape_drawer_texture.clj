(ns cdq.create.shape-drawer-texture
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]))

(defn do! [ctx]
  (assoc ctx :ctx/shape-drawer-texture (let [pixmap (doto (pixmap/create)
                                                      (pixmap/set-color! color/white)
                                                      (pixmap/draw-pixel! 0 0))
                                             texture (texture/create pixmap)]
                                         (pixmap/dispose! pixmap)
                                         texture)))
