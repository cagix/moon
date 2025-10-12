(ns clojure.gdx.graphics.g2d.batch
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn draw!
  ([^Batch batch texture-region x y origin-x origin-y w h scale-x scale-y rotation]
   (.draw batch
          texture-region
          x
          y
          origin-x
          origin-y
          w
          h
          scale-x
          scale-y
          rotation))
  ([^Batch batch texture-region x y w h]
   (.draw batch
          texture-region
          (float x)
          (float y)
          (float w)
          (float h))))
