(ns cdq.game.shape-drawer
  (:require [cdq.ctx :as ctx]
            [cdq.interop :as interop]
            [cdq.shape-drawer :as sd]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx.math MathUtils)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn create []
  (let [this (ShapeDrawer. (:java-object ctx/batch)
                           ctx/shape-drawer-texture)]
    (reify
      sd/ShapeDrawer
      (set-color! [_ color]
        (.setColor this (interop/->color color)))

      (ellipse! [_ x y radius-x radius-y]
        (.ellipse this
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

      (filled-ellipse! [_ x y radius-x radius-y]
        (.filledEllipse this
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

      (circle! [_ x y radius]
        (.circle this
                 (float x)
                 (float y)
                 (float radius)))

      (filled-circle! [_ x y radius]
        (.filledCircle this
                       (float x)
                       (float y)
                       (float radius)))

      (arc! [_ center-x center-y radius start-angle degree]
        (.arc this
              (float center-x)
              (float center-y)
              (float radius)
              (float (degree->radians start-angle))
              (float (degree->radians degree))))

      (sector! [_ center-x center-y radius start-angle degree]
        (.sector this
                 (float center-x)
                 (float center-y)
                 (float radius)
                 (float (degree->radians start-angle))
                 (float (degree->radians degree))))

      (rectangle! [_ x y w h]
        (.rectangle this
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

      (filled-rectangle! [_ x y w h]
        (.filledRectangle this
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

      (line! [_ sx sy ex ey]
        (.line this
               (float sx)
               (float sy)
               (float ex)
               (float ey)))

      (with-line-width [_ width draw-fn]
        (let [old-line-width (.getDefaultLineWidth this)]
          (.setDefaultLineWidth this (float (* width old-line-width)))
          (draw-fn)
          (.setDefaultLineWidth this (float old-line-width)))))))

(defn do! []
  (utils/bind-root #'ctx/shape-drawer (create)))
