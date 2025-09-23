(ns cdq.create.input
  (:require [cdq.input]
            [clojure.input :as input]
            [clojure.math.vector2 :as v]))

; Only access 'clojure.input' through here
; and every key is dispatched here
; so I can create an info text

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}]
  (assert stage)
  (input/set-processor! input stage)
  (assoc ctx :ctx/input input))

(defn- WASD-movement-vector [input]
  (let [r (when (input/key-pressed? input :d) [1  0])
        l (when (input/key-pressed? input :a) [-1 0])
        u (when (input/key-pressed? input :w) [0  1])
        d (when (input/key-pressed? input :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))

(extend-type com.badlogic.gdx.Input
  cdq.input/Input
  (player-movement-vector [input]
    (WASD-movement-vector input)))
