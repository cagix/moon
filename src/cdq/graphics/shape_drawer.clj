(ns cdq.graphics.shape-drawer
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.math.utils :refer [degree->radians]]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer])
  (:import (com.badlogic.gdx.graphics Color)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [{:keys [gdl.graphics/batch
                      gdl.graphics/shape-drawer-texture]
               :as context}
              _config]
  (assoc context :gdl.graphics/shape-drawer
         (sd/create batch (texture-region/create shape-drawer-texture 1 0 1 1))))

(defn- munge-color [c]
  (cond (= com.badlogic.gdx.graphics.Color (class c)) c
        (keyword? c) (interop/k->color c)
        (vector? c) (apply color/create c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn- sd-color [shape-drawer color]
  (sd/set-color shape-drawer (munge-color color)))

(extend-type space.earlygrey.shapedrawer.ShapeDrawer
  gdl.graphics.shape-drawer/ShapeDrawer
  (ellipse [this [x y] radius-x radius-y color]
    (sd-color this color)
    (sd/ellipse this x y radius-x radius-y))

  (filled-ellipse [this [x y] radius-x radius-y color]
    (sd-color this color)
    (sd/filled-ellipse this x y radius-x radius-y))

  (circle [this [x y] radius color]
    (sd-color this color)
    (sd/circle this x y radius))

  (filled-circle [this [x y] radius color]
    (sd-color this color)
    (sd/filled-circle this x y radius))

  (arc [this [center-x center-y] radius start-angle degree color]
    (sd-color this color)
    (sd/arc this center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

  (sector [this [center-x center-y] radius start-angle degree color]
    (sd-color this color)
    (sd/sector this center-x center-y radius (degree->radians start-angle) (degree->radians degree)))

  (rectangle [this x y w h color]
    (sd-color this color)
    (sd/rectangle this x y w h))

  (filled-rectangle [this x y w h color]
    (sd-color this color)
    (sd/filled-rectangle this x y w h))

  (line [this [sx sy] [ex ey] color]
    (sd-color this color)
    (sd/line this sx sy ex ey))

  (with-line-width [this width draw-fn]
    (let [old-line-width (sd/default-line-width this)]
      (sd/set-default-line-width this (* width old-line-width))
      (draw-fn)
      (sd/set-default-line-width this old-line-width))))
