(ns gdl.context
  (:require [clojure.gdx.audio.sound :as sound]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
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

(defn sprite [path]
  (sprite/create {:assets assets
                  :world-unit-scale g/world-unit-scale}
                 path))

(defn sub-sprite [sprite xywh]
  (sprite/sub g/world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [path tilew tileh]
  (sprite/sheet {:assets assets
                 :world-unit-scale g/world-unit-scale}
                path
                tilew
                tileh))

(defn from-sprite-sheet [sprite-sheet xy]
  (sprite/from-sheet g/world-unit-scale
                     sprite-sheet
                     xy))
