(ns cdq.g.player-movement-vector
  (:require [cdq.g :as g]
            [cdq.vector2 :as v]
            gdl.application))

(extend-type gdl.application.Context
  g/PlayerMovementInput
  (player-movement-vector [ctx]
    (let [r (when (g/key-pressed? ctx :d) [1  0])
          l (when (g/key-pressed? ctx :a) [-1 0])
          u (when (g/key-pressed? ctx :w) [0  1])
          d (when (g/key-pressed? ctx :s) [0 -1])]
      (when (or r l u d)
        (let [v (v/add-vs (remove nil? [r l u d]))]
          (when (pos? (v/length v))
            v))))))
