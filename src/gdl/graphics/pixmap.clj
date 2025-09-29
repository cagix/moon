(ns gdl.graphics.pixmap
  (:import (com.badlogic.gdx.graphics Texture
                                      Pixmap
                                      Pixmap$Format)))

(defn set-color! [^Pixmap pixmap [r g b a]]
  (.setColor pixmap r g b a))

(defn draw-pixel! [^Pixmap pixmap x y]
  (.drawPixel pixmap x y))

(defn texture [^Pixmap pixmap]
  (Texture. pixmap))
