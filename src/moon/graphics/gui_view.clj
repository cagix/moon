(ns moon.graphics.gui-view
  (:require [gdl.graphics.viewport :as vp])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(declare ^:private view)

(defn viewport []
  (:viewport view))

(defn init [{:keys [world-width world-height]}]
  (bind-root #'view
             {:unit-scale 1
              :viewport (FitViewport. world-width
                                      world-height
                                      (OrthographicCamera.))}))

(defn resize [dimensions]
  (vp/update (viewport) dimensions :center-camera? true))

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (vp/unproject-mouse-posi (viewport))))

(defn width  [] (vp/world-width  (viewport)))
(defn height [] (vp/world-height (viewport)))
