(ns gdl.context
  (:require [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.graphics.sprite :as sprite]))

(defn sprite [path]
  (sprite/create {:assets assets/manager
                  :world-unit-scale g/world-unit-scale}
                 path))

(defn sub-sprite [sprite xywh]
  (sprite/sub g/world-unit-scale
              sprite
              xywh))

(defn sprite-sheet [path tilew tileh]
  (sprite/sheet {:assets assets/manager
                 :world-unit-scale g/world-unit-scale}
                path
                tilew
                tileh))

(defn from-sprite-sheet [sprite-sheet xy]
  (sprite/from-sheet g/world-unit-scale
                     sprite-sheet
                     xy))
