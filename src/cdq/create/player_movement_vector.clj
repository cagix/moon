(ns cdq.create.player-movement-vector
  (:require [cdq.controls :as controls]
            [gdl.input :as input]
            [gdl.math.vector2 :as v]))

; controls is a ctx element with control config ( key mappings)
; and also holds input
; maybe only exposing input like this in our app
; see also window controls / camera controls
; and can also info-text it properly

(defn- WASD-movement-vector [{:keys [ctx/gdx]}]
  (let [r (when (input/key-pressed? gdx :d) [1  0])
        l (when (input/key-pressed? gdx :a) [-1 0])
        u (when (input/key-pressed? gdx :w) [0  1])
        d (when (input/key-pressed? gdx :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn do! [ctx]
  (extend (class ctx)
    controls/PlayerMovementInput
    {:player-movement-vector WASD-movement-vector})
  ctx)
