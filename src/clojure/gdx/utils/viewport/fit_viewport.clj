(ns clojure.gdx.utils.viewport.fit-viewport
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create ^FitViewport [width height camera]
  (FitViewport. width height camera))
