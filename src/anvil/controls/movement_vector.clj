(ns anvil.controls.movement-vector
  (:require [clojure.gdx :refer [key-pressed?]]
            [anvil.controls :as controls]
            [gdl.math.vector :as v]))

(defn- add-vs [vs]
  (v/normalise (reduce v/add [0 0] vs)))

(defn- WASD-movement-vector [c]
  (let [r (when (key-pressed? c :d) [1  0])
        l (when (key-pressed? c :a) [-1 0])
        u (when (key-pressed? c :w) [0  1])
        d (when (key-pressed? c :s) [0 -1])]
    (when (or r l u d)
      (let [v (add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn-impl controls/movement-vector [c]
  (WASD-movement-vector c))
