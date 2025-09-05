(ns clojure.gdx.graphics.texture
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn create [pixmap]
  (Texture. pixmap))

(defn region [texture x y w h]
  (TextureRegion. texture x y w h))
