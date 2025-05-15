(ns cdq.input
  (:require [cdq.vector2 :as v])
  (:import (com.badlogic.gdx Gdx Input$Keys)))

(defn player-movement-vector []
  (let [r (when (.isKeyPressed Gdx/input Input$Keys/D) [1  0])
        l (when (.isKeyPressed Gdx/input Input$Keys/A) [-1 0])
        u (when (.isKeyPressed Gdx/input Input$Keys/W) [0  1])
        d (when (.isKeyPressed Gdx/input Input$Keys/S) [0 -1])]
    (when (or r l u d)
      (let [v (v/add-vs (remove nil? [r l u d]))]
        (when (pos? (v/length v))
          v)))))
