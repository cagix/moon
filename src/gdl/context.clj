(ns gdl.context
  (:require [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics.sprite :as sprite]))

(declare assets
         batch)

(def sound-asset-format "sounds/%s.wav")

(defn play-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       assets
       sound/play))

(defn setup-world-unit-scale [tile-size]
  (def world-unit-scale (float (/ tile-size))))

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
