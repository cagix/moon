(ns clojure.gdx.utils.viewport
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

(defn update! [viewport width height & {:keys [center?]}]
  (Viewport/.update viewport width height center?))
