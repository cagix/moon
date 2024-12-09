(ns anvil.entity
  (:require [clojure.gdx.math.shapes :as shape]
            [clojure.gdx.math.vector2 :as v]
            [clojure.utils :refer [->tile]]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn tile [entity]
  (->tile (:position entity)))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
