(ns cdq.application.resize
  (:require [cdq.gdx.utils.viewport :as viewport]))

(defn context [context width height]
  (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
  (viewport/update (:cdq.graphics/world-viewport context) width height))
