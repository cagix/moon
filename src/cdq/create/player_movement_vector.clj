(ns cdq.create.player-movement-vector
  (:require [cdq.controls :as controls]
            [cdq.input :as input]
            [cdq.vector2 :as v]))

(defn- WASD-movement-vector [ctx]
  (let [r (when (input/key-pressed? ctx :d) [1  0])
        l (when (input/key-pressed? ctx :a) [-1 0])
        u (when (input/key-pressed? ctx :w) [0  1])
        d (when (input/key-pressed? ctx :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn do! [ctx]
  (extend (class ctx)
    controls/PlayerMovementInput
    {:player-movement-vector WASD-movement-vector})
  ctx)
