(ns com.badlogic.gdx.graphics.g2d.freetype.generator
  (:import (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator)))

(defn create [file-handle]
  (FreeTypeFontGenerator. file-handle))

(defn generate-font [generator parameters]
  (.generateFont generator parameters))
