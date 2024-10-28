(ns moon.graphics
  (:require [moon.assets :as assets])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn tr-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn texture-region
  ([path-or-texture]
   (let [^Texture tex (if (string? path-or-texture)
                        (get assets/manager path-or-texture)
                        path-or-texture)]
     (TextureRegion. tex)))

  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))
