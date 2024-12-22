(ns anvil.controls.movement-vector
  (:require [anvil.controls :as controls]
            [gdl.math.vector :as v]))

(defn- add-vs [vs]
  (v/normalise (reduce v/add [0 0] vs)))

(defn- WASD-movement-vector []
  (let [r (when (key-pressed? :d) [1  0])
        l (when (key-pressed? :a) [-1 0])
        u (when (key-pressed? :w) [0  1])
        d (when (key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn-impl controls/movement-vector []
  (WASD-movement-vector))
