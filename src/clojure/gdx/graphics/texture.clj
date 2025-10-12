(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.graphics Pixmap
                                      Texture)))

(defmulti create type)

(defmethod create String [path]
  (Texture. path))

(defmethod create Pixmap [pixmap]
  (Texture. pixmap))
