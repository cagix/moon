(ns gdl.context
  "Abstract context, the vars have to be defined first - allows dependency-free use of context.

  - this ns is not allowed to have any dependencies ! -

  Why?

  Because otherwise we do not have an abstract 'context' concept.
  "
  (:require [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.utils.viewport :as viewport]
            [gdl.graphics.sprite :as sprite]
            [gdl.tiled :as tiled])
  (:import (forge OrthogonalTiledMapRenderer ColorSetter)))

(declare assets
         batch)

(def sound-asset-format "sounds/%s.wav")

(defn get-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       assets))

(defn play-sound [sound-name]
  (-> sound-name
      get-sound
      sound/play))

(declare world-unit-scale
         world-viewport-width
         world-viewport-height
         world-viewport
         camera)

(defn texture-region [path]
  (texture-region/create (assets path)))

(defn sprite [path]
  (sprite/create world-unit-scale
                 (texture-region path)))

(defn sub-sprite [sprite xywh]
  (sprite/sub world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [path tilew tileh]
  (sprite/sheet world-unit-scale
                (texture-region path)
                tilew
                tileh))

(defn from-sprite-sheet [sprite-sheet xy]
  (sprite/from-sheet world-unit-scale
                     sprite-sheet
                     xy))

(declare default-font)

(declare cursors)

(defn set-cursor [cursor-key]
  (g/set-cursor (safe-get cursors cursor-key)))

(declare viewport
         viewport-width
         viewport-height)

(defn resize-viewport [w h]
  (viewport/update viewport w h :center-camera? true))

(declare tiled-map-renderer)

(defn- draw-tiled-map* [^OrthogonalTiledMapRenderer this tiled-map color-setter camera]
  (.setColorSetter this (reify ColorSetter
                          (apply [_ color x y]
                            (color-setter color x y))))
  (.setView this camera)
  (->> tiled-map
       tiled/layers
       (filter tiled/visible?)
       (map (partial tiled/layer-index tiled-map))
       int-array
       (.render this)))

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (draw-tiled-map* (tiled-map-renderer tiled-map)
                   tiled-map
                   color-setter
                   camera))

(declare shape-drawer)
