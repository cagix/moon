(ns cdq.graphics
  (:require [cdq.ctx :as ctx]
            [cdq.viewport :as viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Texture OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.math Vector2 MathUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      viewport/Viewport
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
        (let [mouse-x (clamp (.getX Gdx/input)
                             (.getLeftGutterWidth this)
                             (.getRightGutterX    this))
              mouse-y (clamp (.getY Gdx/input)
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

(defn ui-viewport [width height]
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


(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defn pixels->world-units [pixels]
  (* (int pixels) ctx/world-unit-scale))

(defn sub-sprite [sprite [x y w h]]
  (sprite* (TextureRegion. ^TextureRegion (:texture-region sprite)
                           (int x)
                           (int y)
                           (int w)
                           (int h))
           ctx/world-unit-scale))

(defn sprite-sheet [texture tilew tileh]
  {:image (sprite* (TextureRegion. ^Texture texture)
                   ctx/world-unit-scale)
   :tilew tilew
   :tileh tileh})

(defn from-sheet [{:keys [image tilew tileh]} [x y]]
  (sub-sprite image [(* x tilew) (* y tileh) tilew tileh]))

(defn sprite [texture]
  (sprite* (TextureRegion. ^Texture texture)
           ctx/world-unit-scale))
