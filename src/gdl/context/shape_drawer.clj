(ns gdl.context.shape-drawer
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [gdl.context :as ctx]))

(defn setup []
  (def sd-texture (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                                 (pixmap/set-color color/white)
                                 (pixmap/draw-pixel 0 0))
                        texture (texture/create pixmap)]
                    (dispose pixmap)
                    texture))
  (bind-root ctx/shape-drawer (sd/create ctx/batch
                                         (texture-region/create sd-texture 1 0 1 1))))

(defn cleanup []
  (dispose sd-texture))

