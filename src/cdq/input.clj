(ns cdq.input
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.math.vector2 :as v]))

(defn player-movement-vector []
  (let [r (when (gdx/key-pressed? :d) [1  0])
        l (when (gdx/key-pressed? :a) [-1 0])
        u (when (gdx/key-pressed? :w) [0  1])
        d (when (gdx/key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))
