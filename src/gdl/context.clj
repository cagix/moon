(ns gdl.context
  (:require [gdl.context.assets :as assets]
            [gdl.graphics.sprite :as sprite]))

(defn setup-world-unit-scale [tile-size]
  (def world-unit-scale (float (/ tile-size))))

(defn sprite [path]
  (sprite/create world-unit-scale
                 (assets/texture-region path)))

(defn sub-sprite [sprite xywh]
  (sprite/sub world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [path tilew tileh]
  (sprite/sheet world-unit-scale
                (assets/texture-region path)
                tilew
                tileh))

(defn from-sprite-sheet [sprite-sheet xy]
  (sprite/from-sheet world-unit-scale
                     sprite-sheet
                     xy))
