(ns cdq.input
  (:require [cdq.vector2 :as v]
            [clojure.input :as input]))

(defn player-movement-vector []
  (let [r (when (input/key-pressed? :d) [1  0])
        l (when (input/key-pressed? :a) [-1 0])
        u (when (input/key-pressed? :w) [0  1])
        d (when (input/key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))
