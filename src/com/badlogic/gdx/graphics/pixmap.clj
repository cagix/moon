(ns com.badlogic.gdx.graphics.pixmap
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format)))

(defn create
  ([]
   (Pixmap. 1 1 Pixmap$Format/RGBA8888))
  ([^FileHandle file-handle]
   (Pixmap. file-handle)))

(defn set-color! [pixmap [r g b a]]
  (Pixmap/.setColor pixmap r g b a))

(defn draw-pixel! [pixmap x y]
  (Pixmap/.drawPixel pixmap x y))

(defn dispose! [pixmap]
  (Pixmap/.dispose pixmap))
