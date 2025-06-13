(ns gdx.utils.viewport
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn update! [^Viewport viewport width height & {:keys [center-camera?]}]
  (.update viewport width height center-camera?))
