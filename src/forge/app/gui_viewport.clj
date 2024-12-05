(ns forge.app.gui-viewport
  (:require [forge.core :refer [bind-root
                                gui-viewport-width
                                gui-viewport-height
                                gui-viewport]])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create [[_ [width height]]]
  (bind-root gui-viewport-width  width)
  (bind-root gui-viewport-height height)
  (bind-root gui-viewport (FitViewport. width height (OrthographicCamera.))))

(defn resize [_ w h]
  (.update gui-viewport w h true))
