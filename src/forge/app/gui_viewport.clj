(ns ^:no-doc forge.app.gui-viewport
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defmethods :app/gui-viewport
  (app-create [[_ [width height]]]
    (bind-root #'gui-viewport-width  width)
    (bind-root #'gui-viewport-height height)
    (bind-root #'gui-viewport (FitViewport. width height (OrthographicCamera.))))
  (app-resize [_ w h]
    (.update gui-viewport w h true)))
