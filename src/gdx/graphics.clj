(ns gdx.graphics
  (:require [com.badlogic.gdx.utils.viewport :as vp]
            [gdl.graphics.viewport]
            [gdl.math :refer [clamp]])
  (:import (com.badlogic.gdx.utils.viewport FitViewport)))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(extend FitViewport
  gdl.graphics.viewport/Viewport
  {:camera vp/camera
   :world-width vp/world-width
   :world-height vp/world-height
   :unproject (fn [this [x y]]
                (vp/unproject this
                              (clamp x
                                     (.getLeftGutterWidth this)
                                     (.getRightGutterX    this))
                              (clamp y
                                     (.getTopGutterHeight this)
                                     (.getTopGutterY      this))))

   :update! vp/update!})
