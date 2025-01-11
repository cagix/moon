(ns clojure.gdx.utils.viewport.fit-viewport
  (:require [clojure.gdx.interop :refer [k->viewport-field]]
            [clojure.gdx.utils.viewport :as viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (gdl.utils Resizable)))

(defn create
  "A ScalingViewport that uses Scaling.fit so it keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the remaining space."
  [width height camera & {:keys [center-camera?]}]
  {:pre [width height]}
  (proxy [FitViewport ILookup Resizable] [width height camera]
    (valAt
      ([key]
       (k->viewport-field this key))
      ([key _not-found]
       (k->viewport-field this key)))
    (resize [width height]
      ; TODO just pipeline the operations
      (viewport/resize this width height :center-camera? center-camera?))))
