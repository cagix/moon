(ns anvil.entity.body
  (:require [anvil.math.vector :as v]
            [anvil.math.shapes :as shape]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn tile [entity]
  (mapv int (:position entity)))

