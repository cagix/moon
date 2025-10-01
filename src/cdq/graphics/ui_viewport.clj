(ns cdq.graphics.ui-viewport
  (:require [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.orthographic-camera :as orthographic-camera]))

(defn create
  [{:keys [graphics/core]
    :as graphics} ui-viewport]
  (assoc graphics :graphics/ui-viewport (graphics/fit-viewport core
                                                               (:width  ui-viewport)
                                                               (:height ui-viewport)
                                                               (orthographic-camera/create))))
