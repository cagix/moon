(ns clojure.gdx.graphics.g2d.freetype.generator
  (:import (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator)))

(defn create-font [file-handle parameters]
  (let [generator (FreeTypeFontGenerator. file-handle)]
    ; TODO dispose generator?
    (.generateFont generator parameters)))
