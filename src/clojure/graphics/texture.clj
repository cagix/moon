(ns clojure.graphics.texture
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture x y w h]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h))))

(defn sub-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region
                  (int x)
                  (int y)
                  (int w)
                  (int h)))

(defn ->sub-region [^Texture texture x y w h]
  (TextureRegion. texture
                  (int x)
                  (int y)
                  (int w)
                  (int h)))
