(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.graphics Texture
                                      Pixmap)))

(defn create ^Texture [^Pixmap pixmap]
  (Texture. pixmap))

(defn dispose! [^Texture texture]
  (.dispose texture))
