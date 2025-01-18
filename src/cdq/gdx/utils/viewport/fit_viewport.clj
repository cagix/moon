(ns cdq.gdx.utils.viewport.fit-viewport
  (:require [cdq.interop :refer [k->viewport-field]])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create
  "A ScalingViewport that uses Scaling.fit so it keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the remaining space."
  [width height camera & {:keys [center-camera?]}]
  {:pre [width height]}
  (proxy [FitViewport ILookup] [width height camera]
    (valAt
      ([key]
       (k->viewport-field this key))
      ([key _not-found]
       (k->viewport-field this key)))))
