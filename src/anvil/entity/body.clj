(ns ^:no-doc anvil.entity.body
  (:require [anvil.entity :as entity]
            [gdl.math.vector :as v]
            [gdl.math.shapes :as shape]
            [gdl.utils :refer [defn-impl]]))

(defn-impl entity/direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn-impl entity/collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn-impl entity/tile [entity]
  (mapv int (:position entity)))

