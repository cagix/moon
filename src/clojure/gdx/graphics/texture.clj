(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn from-file [file-handle] ; needs graphics?
  (Texture. ^FileHandle file-handle))

(defn create [pixmap] ; pixmap/texture?
  (Texture. pixmap))

; use protocol ?
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
