(ns cdq.gdx.math.geom
  (:refer-clojure :exclude [contains?])
  (:require [clojure.gdx.math.circle :as circle]
            [clojure.gdx.math.intersector :as intersector]
            [clojure.gdx.math.rectangle :as rectangle]))

(def circle circle/create)
(def rectangle rectangle/create)
(def overlaps? intersector/overlaps?)
(def contains? rectangle/contains?)

(defn circle->outer-rectangle [{[x y] :position :keys [radius]}]
  (let [radius (float radius)
        size (* radius 2)]
    {:x (- (float x) radius)
     :y (- (float y) radius)
     :width  size
     :height size}))

(defn body->gdx-rectangle [{:keys [body/position body/width body/height]}]
  {:pre [position width height]}
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    (rectangle x y width height)))
