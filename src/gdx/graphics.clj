(ns gdx.graphics
  (:require [clojure.gdx.utils.viewport :as vp]
            [gdl.graphics.viewport])
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(extend FitViewport
  gdl.graphics.viewport/Viewport
  {:camera       vp/camera
   :world-width  vp/world-width
   :world-height vp/world-height
   :unproject    vp/unproject
   :update!      vp/update!})
