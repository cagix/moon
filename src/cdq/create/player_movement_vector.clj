(ns cdq.create.player-movement-vector
  (:require [cdq.controls :as controls]
            [cdq.vector2 :as v]
            [clojure.x :as x]))

; controls is a ctx element with control config ( key mappings)
; and also holds input
; maybe only exposing input like this in our app
; see also window controls / camera controls
; and can also info-text it properly

(defn- WASD-movement-vector [ctx]
  (let [r (when (x/key-pressed? ctx :d) [1  0])
        l (when (x/key-pressed? ctx :a) [-1 0])
        u (when (x/key-pressed? ctx :w) [0  1])
        d (when (x/key-pressed? ctx :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn do! [ctx]
  (extend (class ctx)
    controls/PlayerMovementInput
    {:player-movement-vector WASD-movement-vector})
  ctx)
