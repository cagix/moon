(ns cdq.g.player-movement-vector
  (:require [cdq.g]
            [cdq.vector2 :as v]
            [gdl.input :as input]))

(extend-type cdq.g.Game
  cdq.g/PlayerMovementInput
  (player-movement-vector [_]
    (let [r (when (input/key-pressed? :d) [1  0])
          l (when (input/key-pressed? :a) [-1 0])
          u (when (input/key-pressed? :w) [0  1])
          d (when (input/key-pressed? :s) [0 -1])]
      (when (or r l u d)
        (let [v (v/add-vs (remove nil? [r l u d]))]
          (when (pos? (v/length v))
            v))))))
