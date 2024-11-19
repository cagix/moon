(ns moon.core
  (:require [gdl.graphics :as graphics]
            [gdl.graphics.image :as image]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.text :as text]
            [gdl.graphics.world-view :as world-view]
            [gdl.utils :refer [safe-get]])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.utils.viewport Viewport)))

(declare asset-manager
         batch
         shape-drawer
         cursors)

(def ^:dynamic ^:private *unit-scale* 1)

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
  (text/draw batch *unit-scale* opts))

(defn draw-image [image position]
  (image/draw batch *unit-scale* image position))

(defn draw-centered [image position]
  (image/draw-centered batch *unit-scale* image position))

(defn draw-rotated-centered [image rotation position]
  (image/draw-rotated-centered batch *unit-scale* image rotation position))

(defn draw-ellipse [position radius-x radius-y color]
  (sd/set-color shape-drawer color)
  (sd/ellipse shape-drawer position radius-x radius-y))

(defn draw-filled-ellipse [position radius-x radius-y color]
  (sd/set-color shape-drawer color)
  (sd/filled-ellipse shape-drawer position radius-x radius-y))

(defn draw-circle [position radius color]
  (sd/set-color shape-drawer color)
  (sd/circle shape-drawer position radius))

(defn draw-filled-circle [position radius color]
  (sd/set-color shape-drawer color)
  (sd/filled-circle shape-drawer position radius))

(defn draw-arc [center radius start-angle degree color]
  (sd/set-color shape-drawer color)
  (sd/arc shape-drawer center radius start-angle degree))

(defn draw-sector [center radius start-angle degree color]
  (sd/set-color shape-drawer color)
  (sd/sector shape-drawer center radius start-angle degree))

(defn draw-rectangle [x y w h color]
  (sd/set-color shape-drawer color)
  (sd/rectangle shape-drawer x y w h))

(defn draw-filled-rectangle [x y w h color]
  (sd/set-color shape-drawer color)
  (sd/filled-rectangle shape-drawer x y w h))

(defn draw-line [start end color]
  (sd/set-color shape-drawer color)
  (sd/line shape-drawer start end))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (sd/set-color shape-drawer color)
  (sd/grid shape-drawer leftx bottomy gridw gridh cellw cellh color))

(defn with-line-width [width draw-fn]
  (sd/with-line-width shape-drawer width draw-fn))

(defn draw-with [{:keys [^Viewport viewport unit-scale]} draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined (.getCamera viewport)))
  (.begin batch)
  (with-line-width unit-scale
    #(binding [*unit-scale* unit-scale]
       (draw-fn)))
  (.end batch))

(defn draw-on-world-view [render-fn]
  (draw-with world-view/view render-fn))

(defn set-cursor [cursor-key]
  (graphics/set-cursor (safe-get cursors cursor-key)))
