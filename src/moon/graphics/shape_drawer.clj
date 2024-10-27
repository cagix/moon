(ns moon.graphics.shape-drawer
  (:require [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as d]))

(declare sd)

(defn- set-color [sd color]
  (d/set-color sd (color/munge color)))

(defn ellipse [position radius-x radius-y color]
  (set-color sd color)
  (d/ellipse sd position radius-x radius-y))

(defn filled-ellipse [position radius-x radius-y color]
  (set-color sd color)
  (d/filled-ellipse sd position radius-x radius-y))

(defn circle [position radius color]
  (set-color sd color)
  (d/circle sd position radius))

(defn filled-circle [position radius color]
  (set-color sd color)
  (d/filled-circle sd position radius))

(defn arc [center radius start-angle degree color]
  (set-color sd color)
  (d/arc sd center radius start-angle degree))

(defn sector [center radius start-angle degree color]
  (set-color sd color)
  (d/sector sd center radius start-angle degree))

(defn rectangle [x y w h color]
  (set-color sd color)
  (d/rectangle sd x y w h))

(defn filled-rectangle [x y w h color]
  (set-color sd color)
  (d/filled-rectangle sd x y w h))

(defn line [start end color]
  (set-color sd color)
  (d/line sd start end))

(defn grid [leftx bottomy gridw gridh cellw cellh color]
  (set-color sd color)
  (d/grid sd leftx bottomy gridw gridh cellw cellh))

(defn with-line-width [width draw-fn]
  (d/with-line-width sd width draw-fn))
