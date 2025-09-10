(ns cdq.gdx.graphics
  (:require [cdq.utils :as utils]
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]))

(defn draw-texture-region!
  [{:keys [ctx/batch
           ctx/unit-scale
           ctx/world-unit-scale]}
   texture-region
   [x y]
   & {:keys [center? rotation]}]
  (let [[w h] (let [dimensions (texture-region/dimensions texture-region)]
                (if (= @unit-scale 1)
                  dimensions
                  (mapv (comp float (partial * world-unit-scale))
                        dimensions)))]
    (if center?
      (batch/draw! batch
                   texture-region
                   (- (float x) (/ (float w) 2))
                   (- (float y) (/ (float h) 2))
                   [w h]
                   (or rotation 0))
      (batch/draw! batch
                   texture-region
                   x
                   y
                   [w h]
                   0))))

(defn draw-arc!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))

(defn draw-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/circle! shape-drawer x y radius))

(defn draw-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-circle! shape-drawer x y radius))

(defn draw-filled-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-rectangle! shape-drawer x y w h))

(defn draw-line!
  [{:keys [ctx/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/line! shape-drawer sx sy ex ey))

(defn draw-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/rectangle! shape-drawer x y w h))

(defn draw-sector!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (utils/degree->radians start-angle)
              (utils/degree->radians degree)))

(defn draw-text!
  [{:keys [ctx/batch
           ctx/unit-scale
           ctx/default-font]}
   {:keys [font scale x y text h-align up?]}]
  (bitmap-font/draw! (or font default-font)
                     batch
                     {:scale (* (float @unit-scale)
                                (float (or scale 1)))
                      :text text
                      :x x
                      :y y
                      :up? up?
                      :h-align h-align
                      :target-width 0
                      :wrap? false}))

(defn with-line-width
  [{:keys [ctx/shape-drawer]} width f]
  (sd/with-line-width shape-drawer width f))

(defn draw-on-world-viewport!
  [{:keys [ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]}
   f]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1)))
  (batch/end! batch))
