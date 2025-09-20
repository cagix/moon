(ns cdq.application.create.graphics.shape-drawer
  (:require [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.texture :as texture]
            [gdl.graphics.shape-drawer])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn do!
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (ShapeDrawer. batch (texture/region shape-drawer-texture 1 0 1 1))))

(extend-type ShapeDrawer
  gdl.graphics.shape-drawer/ShapeDrawer
  (set-color! [this color]
    (.setColor this (color/create color)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width))))

  (arc! [this center-x center-y radius start-radians radians]
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float start-radians)
          (float radians)))

  (circle! [this x y radius]
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (ellipse! [this x y radius-x radius-y]
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-circle! [this x y radius]
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (filled-ellipse! [this x y radius-x radius-y]
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (filled-rectangle! [this x y w h]
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line! [this sx sy ex ey]
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (rectangle! [this x y w h]
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (sector! [this center-x center-y radius start-radians radians]
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float start-radians)
             (float radians))))
