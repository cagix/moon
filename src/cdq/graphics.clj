(ns cdq.graphics
  (:require [cdq.batch :as batch]
            [cdq.camera :as camera]
            [cdq.ctx :as ctx]
            [cdq.font :as font]
            [cdq.shape-drawer :as sd]
            [cdq.viewport :as viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Texture OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.math Vector2 MathUtils)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn sprite-batch []
  (let [this (SpriteBatch.)]
    (reify
      batch/Batch
      (draw-on-viewport! [_ viewport draw-fn]
        (.setColor this Color/WHITE) ; fix scene2d.ui.tooltip flickering
        (.setProjectionMatrix this (camera/combined (:camera viewport)))
        (.begin this)
        (draw-fn)
        (.end this))

      (draw-texture-region! [_ texture-region [x y] [w h] rotation color]
        (if color (.setColor this color))
        (.draw this
               texture-region
               x
               y
               (/ (float w) 2) ; rotation origin
               (/ (float h) 2)
               w
               h
               1 ; scale-x
               1 ; scale-y
               rotation)
        (if color (.setColor this Color/WHITE)))

      Disposable
      (dispose [_]
        (.dispose this))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this)))))

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

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

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

(defn draw-image [{:keys [texture-region color] :as image} position]
  (batch/draw-texture-region! ctx/batch
                              texture-region
                              position
                              (unit-dimensions image @ctx/unit-scale)
                              0 ; rotation
                              color))

(defn draw-rotated-centered [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image @ctx/unit-scale)]
    (batch/draw-texture-region! ctx/batch
                                texture-region
                                [(- (float x) (/ (float w) 2))
                                 (- (float y) (/ (float h) 2))]
                                [w h]
                                rotation
                                color)))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font scale x y text h-align up?]}]
  (font/draw-text! (or font ctx/default-font)
                   ctx/batch
                   {:scale (* (float @ctx/unit-scale)
                              (float (or scale 1)))
                    :x x
                    :y y
                    :text text
                    :h-align h-align
                    :up? up?}))

(defn draw-ellipse [[x y] radius-x radius-y color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/ellipse! ctx/shape-drawer x y radius-x radius-y))

(defn draw-filled-ellipse [[x y] radius-x radius-y color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/filled-ellipse! ctx/shape-drawer x y radius-x radius-y))

(defn draw-circle [[x y] radius color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/circle! ctx/shape-drawer x y radius))

(defn draw-filled-circle [[x y] radius color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/filled-circle! ctx/shape-drawer x y radius))

(defn draw-arc [[center-x center-y] radius start-angle degree color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/arc! ctx/shape-drawer center-x center-y radius start-angle degree))

(defn draw-sector [[center-x center-y] radius start-angle degree color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/sector! ctx/shape-drawer center-x center-y radius start-angle degree))

(defn draw-rectangle [x y w h color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/rectangle! ctx/shape-drawer x y w h))

(defn draw-filled-rectangle [x y w h color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/filled-rectangle! ctx/shape-drawer x y w h))

(defn draw-line [[sx sy] [ex ey] color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/line! ctx/shape-drawer sx sy ex ey))

(defn with-line-width [width draw-fn]
  (sd/with-line-width ctx/shape-drawer width draw-fn))

(defn draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw-line [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw-line [leftx liney] [rightx liney] color))))

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

(defn draw-centered [image position]
  (draw-rotated-centered image 0 position))
