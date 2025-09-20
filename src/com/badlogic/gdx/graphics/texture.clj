(ns com.badlogic.gdx.graphics.texture
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture
                                      Pixmap)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn from-file [file-handle]
  (Texture. ^FileHandle file-handle))

(defn create [pixmap]
  (Texture. ^Pixmap pixmap))

(defn region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture [x y w h]]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h)))
  ([^Texture texture x y w h]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h))))
