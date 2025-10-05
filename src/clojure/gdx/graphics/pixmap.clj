(ns clojure.gdx.graphics.pixmap
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format
                                      Texture)))

(defn create
  ([^FileHandle file-handle]
   (Pixmap. file-handle))

  ([width height pixmap-format]
   (Pixmap. (int width)
            (int height)
            (case pixmap-format
              :pixmap.format/RGBA8888 Pixmap$Format/RGBA8888))))

(defn set-color! [^Pixmap pixmap [r g b a]]
  (.setColor pixmap r g b a))

(defn draw-pixel! [^Pixmap pixmap x y]
  (.drawPixel pixmap x y))

(defn texture [^Pixmap pixmap]
  (Texture. pixmap))
