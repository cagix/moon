(ns cdq.graphics.camera
  (:require [clojure.gdx.graphics.orthographic-camera :as camera]
            [clojure.gdx.math.vector3 :as vector3]))

(defn viewport-height [this]
  (camera/viewport-height this))

(defn viewport-width [this]
  (camera/viewport-width this))

(defn position [this]
  (vector3/clojurize (camera/position this)))

(defn zoom [this]
  (camera/zoom this))

(defn combined [this]
  (camera/combined this))

(defn set-position! [this position]
  (camera/set-position! this position)
  (camera/update! this))

(defn set-zoom! [this amount]
  (camera/set-zoom! this amount)
  (camera/update! this))

(defn frustum-bounds [this]
  (mapv vector3/clojurize (.planePoints (camera/frustum this))))

(defn reset-zoom! [cam]
  (set-zoom! cam 1))

(defn inc-zoom! [cam by]
  (set-zoom! cam (max 0.1 (+ (zoom cam) by))))

(defn frustum [camera]
  (let [plane-points (frustum-bounds camera)
        frustum-points (take 4 plane-points)
        left-x   (apply min (map first  frustum-points))
        right-x  (apply max (map first  frustum-points))
        bottom-y (apply min (map second frustum-points))
        top-y    (apply max (map second frustum-points))]
    [left-x right-x bottom-y top-y]))

(defn visible-tiles [camera]
  (let [[left-x right-x bottom-y top-y] (frustum camera)]
    (for [x (range (int left-x)   (int right-x))
          y (range (int bottom-y) (+ 2 (int top-y)))]
      [x y])))

(defn calculate-zoom
  "calculates the zoom value for camera to see all the 4 points."
  [camera & {:keys [left top right bottom]}]
  (let [viewport-width  (viewport-width  camera)
        viewport-height (viewport-height camera)
        [px py] (position camera)
        px (float px)
        py (float py)
        leftx (float (left 0))
        rightx (float (right 0))
        x-diff (max (- px leftx) (- rightx px))
        topy (float (top 1))
        bottomy (float (bottom 1))
        y-diff (max (- topy py) (- py bottomy))
        vp-ratio-w (/ (* x-diff 2) viewport-width)
        vp-ratio-h (/ (* y-diff 2) viewport-height)
        new-zoom (max vp-ratio-w vp-ratio-h)]
    new-zoom))
