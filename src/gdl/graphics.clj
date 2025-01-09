(ns gdl.graphics
  (:require [clojure.files :as files]
            [clojure.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.utils.screen :as screen]
            [gdl.ui :as ui]
            [clojure.utils :refer [mapvals]])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn- create-cursors [{:keys [clojure.gdx/files
                               clojure.gdx/graphics]} cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (pixmap/create (files/internal files (str "cursors/" file ".png")))
                   cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
               (.dispose pixmap)
               cursor))
           cursors))

(defn create [context {:keys [cursors]}]
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
     :cursors (create-cursors context cursors)
     }))

(defn clear-screen [context]
  (screen/clear color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)
