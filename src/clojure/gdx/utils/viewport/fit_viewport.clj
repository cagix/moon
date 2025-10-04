(ns clojure.gdx.utils.viewport.fit-viewport
  (:require [clojure.gdx.utils.viewport :as vp]
            [clojure.graphics.viewport])
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create [width height camera]
  (FitViewport. width height camera))

(extend FitViewport
  clojure.graphics.viewport/Viewport
  {:camera       vp/camera
   :world-width  vp/world-width
   :world-height vp/world-height
   :unproject    vp/unproject
   :update!      vp/update!})
