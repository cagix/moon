(ns gdl.context.shape-drawer
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.pixmap :as pixmap]))

(defn- sd-texture []
  (let [pixmap (doto (gdx/pixmap 1 1 pixmap/format-RGBA8888)
                 (pixmap/set-color gdx/white)
                 (gdx/draw-pixel 0 0))
        texture (gdx/texture pixmap)]
    (gdx/dispose pixmap)
    texture))

(defn create [_ {:keys [gdl.context/batch]}]
  (assert batch)
  (sd/create batch (gdx/texture-region (sd-texture) 1 0 1 1)))

(defn dispose [[_ sd]]
  #_(gdx/dispose sd))
; TODO this will break ... proxy with extra-data -> get texture through sd ...
; => shape-drawer-texture as separate component?!
; that would work
