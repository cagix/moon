(ns moon.graphics.world-view
  (:require [gdl.graphics.viewport :as vp]
            [moon.app :as app]
            [moon.graphics.view :as view])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(declare ^:private view)

(defn- viewport [] (:viewport view))

(defc :moon.graphics.world-view
  (app/create [[_ {:keys [world-width world-height tile-size]}]]
    (bind-root #'view
               (let [unit-scale (/ tile-size)]
                 {:unit-scale (float unit-scale)
                  :viewport (let [world-width  (* world-width  unit-scale)
                                  world-height (* world-height unit-scale)
                                  camera (OrthographicCamera.)
                                  y-down? false]
                              (.setToOrtho camera y-down? world-width world-height)
                              (FitViewport. world-width world-height camera))})))

  (app/resize [_ dimensions]
    (vp/update (viewport) dimensions)))

(defn unit-scale [] (:unit-scale view))

(defn pixels->units [pixels]
  (* (int pixels) (unit-scale)))

(defn mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (vp/unproject-mouse-posi (viewport)))

(defn camera [] (vp/camera       (viewport)))
(defn width  [] (vp/world-width  (viewport)))
(defn height [] (vp/world-height (viewport)))

(defn render [render-fn]
  (view/render view render-fn))
