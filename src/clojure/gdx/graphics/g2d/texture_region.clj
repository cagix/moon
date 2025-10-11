(ns clojure.gdx.graphics.g2d.texture-region
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn create
  ([texture]
   (TextureRegion. texture))
  ([texture [x y w h]]
   (TextureRegion. texture x y w h)))
