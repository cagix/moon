(ns cdq.input
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.input :as input]
            [clojure.gdx.math.vector2 :as v]))

(defn player-movement-vector []
  (let [r (when (input/key-pressed? gdx/input :d) [1  0])
        l (when (input/key-pressed? gdx/input :a) [-1 0])
        u (when (input/key-pressed? gdx/input :w) [0  1])
        d (when (input/key-pressed? gdx/input :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))
