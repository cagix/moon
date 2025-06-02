(ns clojure.create.player-movement-vector
  (:require [clojure.controls :as controls]
            [clojure.vector2 :as v]
            [clojure.input :as input]))

; controls is a ctx element with control config ( key mappings)
; and also holds input
; maybe only exposing input like this in our app
; see also window controls / camera controls
; and can also info-text it properly

(defn- WASD-movement-vector [{:keys [ctx/input]}]
  (let [r (when (input/key-pressed? input :d) [1  0])
        l (when (input/key-pressed? input :a) [-1 0])
        u (when (input/key-pressed? input :w) [0  1])
        d (when (input/key-pressed? input :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(defn do! [ctx]
  (extend (class ctx)
    controls/PlayerMovementInput
    {:player-movement-vector WASD-movement-vector})
  ctx)
