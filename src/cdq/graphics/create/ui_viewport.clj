(ns cdq.graphics.create.ui-viewport
  (:require [clojure.gdx.graphics.orthographic-camera :as orthographic-camera])
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create
  [{:keys [graphics/core]
    :as graphics} ui-viewport]
  (assoc graphics :graphics/ui-viewport (FitViewport. (:width  ui-viewport)
                                                      (:height ui-viewport)
                                                      (orthographic-camera/create))))
