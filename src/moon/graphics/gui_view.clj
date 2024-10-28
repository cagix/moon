(ns moon.graphics.gui-view
  (:require [gdl.graphics.viewport :as vp]
            [moon.app :as app]
            [moon.component :refer [defc]])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(declare ^:private view)

(defn viewport []
  (:viewport view))

(defc :moon.graphics.gui-view
  (app/create [[_ {:keys [world-width world-height]}]]
    (bind-root #'view
             {:unit-scale 1
              :viewport (FitViewport. world-width
                                      world-height
                                      (OrthographicCamera.))}))

  (app/resize [_ dimensions]
    (vp/update (viewport) dimensions :center-camera? true)))

(defn mouse-position []
  ; TODO mapv int needed?
  (mapv int (vp/unproject-mouse-posi (viewport))))

(defn width  [] (vp/world-width  (viewport)))
(defn height [] (vp/world-height (viewport)))
