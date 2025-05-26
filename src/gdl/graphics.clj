(ns gdl.graphics
  (:import (com.badlogic.gdx.graphics Color
                                      Colors
                                      Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn color [r g b a]
  (Color. (float r)
          (float g)
          (float b)
          (float a)))

(def white Color/WHITE)
(def black Color/BLACK)

(defn def-markdown-color [name color]
  (Colors/put name color))

(defn texture-region [^Texture texture x y w h]
  (TextureRegion. texture
                  (int x)
                  (int y)
                  (int w)
                  (int h)))
