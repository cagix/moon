(ns clojure.gdx.graphics.pixmap
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap
                                      Pixmap$Format)))

(defn create ^Pixmap
  ([^FileHandle file-handle]
   (Pixmap. file-handle))
  ([width height]
   (Pixmap. 1 1 Pixmap$Format/RGBA8888)))
