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
            [gdl.graphics.sprite :as sprite]))

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
