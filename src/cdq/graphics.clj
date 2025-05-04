(ns cdq.graphics
  (:require [cdq.gdx.interop :as interop]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.shape-drawer :as shape-drawer]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont)
           (com.badlogic.gdx.math Vector2 MathUtils)
           (com.badlogic.gdx.utils.viewport Viewport)))

(defn- clamp [value min max]
  (MathUtils/clamp (float value) (float min) (float max)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn- unproject-mouse-position
  "Returns vector of [x y]."
  [viewport]
  (let [mouse-x (clamp (.getX Gdx/input)
                       (:left-gutter-width viewport)
                       (:right-gutter-x    viewport))
        mouse-y (clamp (.getY Gdx/input)
                       (:top-gutter-height viewport)
                       (:top-gutter-y      viewport))]
    (let [v2 (Viewport/.unproject viewport (Vector2. mouse-x mouse-y))]
      [(.x v2) (.y v2)])))

(defn mouse-position [ui-viewport]
  ; TODO mapv int needed?
  (mapv int (unproject-mouse-position ui-viewport)))

(defn world-mouse-position [world-viewport]
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))

(defn pixels->world-units [{:keys [cdq.graphics/world-unit-scale]} pixels]
  (* (int pixels) world-unit-scale))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- draw-texture-region [^Batch batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
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
  (if color (.setColor batch Color/WHITE)))

(defn draw-image
  [{:keys [cdq.context/unit-scale
           cdq.graphics/batch]}
   {:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [cdq.context/unit-scale
           cdq.graphics/batch]}
   {:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image unit-scale)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [c image position]
  (draw-rotated-centered c image 0 position))

(defn set-camera-position! [{:keys [cdq.graphics/world-viewport]} position]
  (camera/set-position! (:camera world-viewport) position))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (BitmapFont/.getLineHeight font))))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [cdq.context/unit-scale
           cdq.graphics/batch
           cdq.graphics/default-font]}
   {:keys [font x y text h-align up? scale]}]
  {:pre [unit-scale]}
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))
        new-scale (float (* old-scale
                            (float unit-scale)
                            (float (or scale 1))))
        target-width (float 0)
        wrap? false]
    (.setScale data new-scale)
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (.setScale data old-scale)))

(defn draw-on-world-view! [{:keys [^Batch cdq.graphics/batch
                                   cdq.graphics/world-viewport
                                   cdq.graphics/world-unit-scale]
                            :as context}
                           draw-fns]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
  (.begin batch)
  (shape-drawer/with-line-width world-unit-scale
    (fn []
      (let [context (assoc context :cdq.context/unit-scale world-unit-scale)]
        (doseq [f draw-fns]
          (f context)))))
  (.end batch))
