(ns moon.core
  (:require [gdl.graphics.image :as image]
            [gdl.graphics.text :as text]
            [gdl.graphics.view :as view]
            [gdl.graphics.world-view :as world-view])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(declare asset-manager
         batch)

(defn play-sound [path]
  (Sound/.play (get asset-manager path)))

(defn texture-region [path]
  (TextureRegion. ^Texture (get asset-manager path)))

(defn image [path]
  (image/create (texture-region path)))

(defn sprite-sheet [path tilew tileh]
  {:image (image path)
   :tilew tilew
   :tileh tileh})

(defn draw-text [opts]
  (text/draw batch opts))

(defn draw-image [image position]
  (image/draw batch image position))

(defn draw-centered [image position]
  (image/draw-centered batch image position))

(defn draw-rotated-centered [image rotation position]
  (image/draw-rotated-centered batch image rotation position))

(defn draw-on-world-view [render-fn]
  (view/render batch world-view/view render-fn))
