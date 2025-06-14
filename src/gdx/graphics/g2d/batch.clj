(ns gdx.graphics.g2d.batch
  (:require [gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.graphics.g2d Batch)
           (com.badlogic.gdx.utils.viewport Viewport)))

(defn draw-on-viewport! [^Batch batch viewport f]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (.setColor batch (color/->obj :white))
  (.setProjectionMatrix batch (.combined (Viewport/.getCamera viewport)))
  (.begin batch)
  (f)
  (.end batch))

(defn draw! [^Batch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))
