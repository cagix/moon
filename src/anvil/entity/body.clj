(ns anvil.entity.body
  (:require [gdl.math.vector :as v]
            [gdl.math.shapes :as shape]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn tile [entity]
  (mapv int (:position entity)))

