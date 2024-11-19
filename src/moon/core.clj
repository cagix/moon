(ns moon.core
  (:require [gdl.graphics.image :as image]
            [gdl.graphics.text :as text]
            [gdl.graphics.view :as view]
            [gdl.graphics.world-view :as world-view]))

(declare batch)

(defn draw-text [opts]
  (text/draw batch opts))

(defn draw-image [image position]
  (image/draw batch image position))

(defn draw-centered [image position]
  (image/draw-centered batch image position))

(defn draw-rotated-centered [image rotation position]
  (image/draw-rotated-centered batch image rotation position))

(defn draw-on-world-view [render-fn]
  (view/render batch world-view/view render-fn))
