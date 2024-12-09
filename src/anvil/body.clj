(ns anvil.body
  (:require [clojure.gdx.math.shapes :as shape]
            [clojure.gdx.math.vector2 :as v]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn tile [entity]
  (mapv int (:position entity)))

