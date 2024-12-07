(ns forge.entity.body
  (:require [clojure.gdx.math.shapes :as shape]
            [clojure.gdx.math.vector2 :as v]
            [forge.utils :refer [->tile]]))

(defn e-direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn e-collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn e-tile [entity]
  (->tile (:position entity)))
