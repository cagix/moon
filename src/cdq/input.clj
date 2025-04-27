(ns cdq.input
  (:require [cdq.gdx.interop :refer [k->input-button k->input-key]]
            [cdq.math.vector2 :as v])
  (:import (com.badlogic.gdx Gdx)))

(defn button-just-pressed? [button]
  (.isButtonJustPressed Gdx/input (k->input-button button)))

(defn key-just-pressed? [key]
  (.isKeyJustPressed Gdx/input (k->input-key key)))

(defn key-pressed? [key]
  (.isKeyPressed Gdx/input (k->input-key key)))

(defn player-movement-vector []
  (let [r (when (key-pressed? :d) [1  0])
        l (when (key-pressed? :a) [-1 0])
        u (when (key-pressed? :w) [0  1])
        d (when (key-pressed? :s) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))
