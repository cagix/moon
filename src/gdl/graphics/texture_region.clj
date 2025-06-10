(ns gdl.graphics.texture-region
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn dimensions [^TextureRegion this]
  [(.getRegionWidth  this)
   (.getRegionHeight this)])

(defprotocol ToTextureRegion
  (convert [_ x y w h]))

(extend-protocol ToTextureRegion
  Texture
  (convert [texture x y w h]
    (TextureRegion. texture
                    (int x)
                    (int y)
                    (int w)
                    (int h)))

  TextureRegion
  (convert [texture-region x y w h]
    (TextureRegion. texture-region
                    (int x)
                    (int y)
                    (int w)
                    (int h))))

(defn create
  ([^Texture texture]
   (TextureRegion. texture))
  ([texture-or-region x y w h]
   (convert texture-or-region x y w h)))
