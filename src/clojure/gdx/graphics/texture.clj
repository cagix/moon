(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.graphics Texture
                                      Pixmap)))

(defn create [^Pixmap pixmap]
  (Texture. pixmap))
