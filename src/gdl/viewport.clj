(ns gdl.viewport
  (:require [clojure.gdx.math.math-utils :as math-utils])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics OrthographicCamera)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defprotocol Viewport
  (update! [_])
  (mouse-position [_]))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      Viewport
      (update! [_]
        (.update this
                 (.getWidth  Gdx/graphics)
                 (.getHeight Gdx/graphics)
                 center-camera?))

      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (mouse-position [_]
        (let [mouse-x (math-utils/clamp (.getX Gdx/input)
                                        (.getLeftGutterWidth this)
                                        (.getRightGutterX    this))
              mouse-y (math-utils/clamp (.getY Gdx/input)
                                        (.getTopGutterHeight this)
                                        (.getTopGutterY      this))]
          (let [v2 (.unproject this (Vector2. mouse-x mouse-y))]
            [(.x v2) (.y v2)])))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (.getCamera      this))))))

(defn ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (OrthographicCamera.)
                {:center-camera? true}))

(defn world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width
                  world-height
                  camera
                  {:center-camera? false})))
