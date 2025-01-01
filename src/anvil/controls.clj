(ns anvil.controls
  (:require [clojure.gdx :refer [key-pressed? key-just-pressed?]]
            [gdl.math.vector :as v]))

(defn unpaused? [c]
  (or (key-just-pressed? c :p)
      (key-pressed? c :space)))

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

(defn movement-vector [c]
  (WASD-movement-vector c))

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")
