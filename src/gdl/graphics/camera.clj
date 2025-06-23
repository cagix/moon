(ns gdl.graphics.camera)

(defprotocol Camera
  (zoom [_])
  (position [_] "Returns camera position as [x y] vector.")
  (frustum [_] "Returns `[left-x right-x bottom-y top-y]`.")
  (set-position! [_ [x y]])
  (set-zoom! [_ value] "Initial zoom is `1`.")
  (viewport-width [_])
  (viewport-height [_]))

(defn reset-zoom! [cam]
  (set-zoom! cam 1))

(defn inc-zoom! [cam by]
  (set-zoom! cam (max 0.1 (+ (zoom cam) by))))
