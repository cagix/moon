(ns ^:no-doc forge.app.gui-viewport
  (:require [forge.system :as system :refer [bind-root defmethods]])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defmethods :app/gui-viewport
  (system/create [[_ [width height]]]
    (bind-root #'system/gui-viewport-width  width)
    (bind-root #'system/gui-viewport-height height)
    (bind-root #'system/gui-viewport (FitViewport. width height (OrthographicCamera.))))
  (system/resize [_ w h]
    (.update system/gui-viewport w h true)))
