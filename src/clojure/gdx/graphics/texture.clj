(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

; clojure.gdx.graphics/texture

(defn from-file [file-handle]
  (Texture. ^FileHandle file-handle))

(defn create [pixmap]
  (Texture. pixmap))

(defn region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture [x y w h]]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h)))
  ([texture x y w h]
   (TextureRegion. texture x y w h)))
