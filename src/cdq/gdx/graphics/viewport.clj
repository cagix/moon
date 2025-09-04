(ns cdq.gdx.graphics.viewport
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn fit [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :viewport/width  (FitViewport/.getWorldWidth  this)
        :viewport/height (FitViewport/.getWorldHeight this)
        :viewport/camera (FitViewport/.getCamera      this)))))
