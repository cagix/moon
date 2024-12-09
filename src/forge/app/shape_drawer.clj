(ns forge.app.shape-drawer
  (:require [anvil.graphics :refer [batch sd]]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [bind-root]]))

(declare ^:private pixel-texture)

(defn- white-pixel-texture []
  (let [pixmap (doto (g/pixmap 1 1)
                 (.setColor color/white)
                 (.drawPixel 0 0))
        texture (g/texture pixmap)]
    (dispose pixmap)
    texture))

(defn create [_]
  (bind-root pixel-texture (white-pixel-texture))
  (bind-root sd (sd/create batch (g/texture-region pixel-texture 1 0 1 1))))

(defn destroy [_]
  (dispose pixel-texture))
