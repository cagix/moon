(ns cdq.g.player-movement-vector
  (:require [cdq.g :as g]
            [cdq.vector2 :as v]
            gdl.application
            [gdl.input :as input]))

(extend-type gdl.application.Context
  g/PlayerMovementInput
  (player-movement-vector [{:keys [ctx/input]}]
    (let [r (when (input/key-pressed? input :d) [1  0])
          l (when (input/key-pressed? input :a) [-1 0])
          u (when (input/key-pressed? input :w) [0  1])
          d (when (input/key-pressed? input :s) [0 -1])]
      (when (or r l u d)
        (let [v (v/add-vs (remove nil? [r l u d]))]
          (when (pos? (v/length v))
            v))))))
