(ns com.badlogic.gdx.graphics.texture
  (:import (com.badlogic.gdx.graphics Texture
                                      Pixmap)))

(defn create [pixmap]
  (Texture. ^Pixmap pixmap))
