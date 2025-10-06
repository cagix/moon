(ns cdq.graphics.create.ui-viewport
  (:require [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]))

(defn create
  [{:keys [graphics/core]
    :as graphics} ui-viewport]
  (assoc graphics :graphics/ui-viewport (fit-viewport/create (:width  ui-viewport)
                                                             (:height ui-viewport)
                                                             (orthographic-camera/create))))
