(ns gdl.context
  (:require [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.assets :as assets]
            [gdl.graphics.sprite :as sprite]))

(def assets-folder "resources/")
(def sound-asset-format "sounds/%s.wav")

(defn assets-setup []
  (def assets (assets/manager assets-folder)))

(defn assets-cleanup []
  (assets/cleanup assets))

(defn play-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       assets
       sound/play))

(defn texture-region [path]
  (texture-region/create (assets path)))

(defn setup-world-unit-scale [tile-size]
  (def world-unit-scale (float (/ tile-size))))

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
