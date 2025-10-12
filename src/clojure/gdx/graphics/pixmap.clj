(ns clojure.gdx.graphics.pixmap
  (:import (com.badlogic.gdx.graphics Pixmap)))

(defn create
  ([file-handle]
   (Pixmap. file-handle))
  ([width height pixmap-format]
   (Pixmap. width height pixmap-format)))

(defn dispose! [pixmap]
  (.dispose pixmap))

(defn set-color! [pixmap color]
  (.setColor pixmap color))

(defn draw-pixel! [pixmap x y]
  (.drawPixel pixmap 0 0))
