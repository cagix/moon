(ns clojure.graphics.texture
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defprotocol Texture
  (region [_]
          [_ x y w h]))

(defn sub-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region
                  (int x)
                  (int y)
                  (int w)
                  (int h)))

(defn ->sub-region [^com.badlogic.gdx.graphics.Texture texture x y w h]
  (TextureRegion. texture
                  (int x)
                  (int y)
                  (int w)
                  (int h)))
