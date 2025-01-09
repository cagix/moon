(ns gdl.graphics
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.utils.screen :as screen]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn create []
  (let [batch (SpriteBatch.)
        sd-texture (let [pixmap (doto (pixmap/create 1 1 pixmap/format-RGBA8888)
                                  (pixmap/set-color color/white)
                                  (pixmap/draw-pixel 0 0))
                         texture (texture/create pixmap)]
                     (.dispose pixmap)
                     texture)]
    {:batch batch
     :sd (sd/create batch (texture-region/create sd-texture 1 0 1 1))
     :sd-texture sd-texture
     }))

(defn clear-screen [context]
  (screen/clear color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)
