(ns cdq.graphics.shape-drawer-texture
  (:require [clojure.graphics.color :as color]
            [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [gdl.disposable :refer [dispose!]]))

(defn create
  [{:keys [graphics/core]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (graphics/pixmap core 1 1 :pixmap.format/RGBA8888)
                                                                (pixmap/set-color! color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (pixmap/texture pixmap)]
                                                   (dispose! pixmap)
                                                   texture)))
