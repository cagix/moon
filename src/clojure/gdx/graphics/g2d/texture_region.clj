(ns clojure.gdx.graphics.g2d.texture-region
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn dimensions [^TextureRegion this]
  [(.getRegionWidth  this)
   (.getRegionHeight this)])

(defn create [^TextureRegion this x y w h]
  (TextureRegion. this
                  (int x)
                  (int y)
                  (int w)
                  (int h)))
