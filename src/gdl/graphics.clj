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
  (:import (com.badlogic.gdx.graphics Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)))

(defn- ttf-params [size quality-scaling]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) (* size quality-scaling))
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [{:keys [file size quality-scaling]}]
  (let [generator (FreeTypeFontGenerator. file)
        font (.generateFont generator (ttf-params size quality-scaling))]
    (.dispose generator)
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- create-cursors [{:keys [clojure.gdx/files
                               clojure.gdx/graphics]} cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (pixmap/create (files/internal files (str "cursors/" file ".png")))
                   cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
               (.dispose pixmap)
               cursor))
           cursors))

(defn create [{:keys [clojure.gdx/files] :as context} config]
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
     :cursors (create-cursors context (:cursors config))
     :default-font (generate-font (update (:default-font config) :file #(files/internal files %)))
     }))

(defn clear-screen [context]
  (screen/clear color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)
