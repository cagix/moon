(ns cdq.g.player-movement-vector
  (:require cdq.gdx
            [cdq.c :as c]
            [cdq.g :as g]
            [cdq.vector2 :as v]))

(extend-type cdq.gdx.Gdx
  g/PlayerMovementInput
  (player-movement-vector [ctx]
    (let [r (when (c/key-pressed? ctx :d) [1  0])
          l (when (c/key-pressed? ctx :a) [-1 0])
          u (when (c/key-pressed? ctx :w) [0  1])
          d (when (c/key-pressed? ctx :s) [0 -1])]
      (when (or r l u d)
        (let [v (v/add-vs (remove nil? [r l u d]))]
          (when (pos? (v/length v))
            v))))))
