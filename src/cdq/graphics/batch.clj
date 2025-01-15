(ns cdq.graphics.batch
  (:require [clojure.graphics.color :as color]
            [clojure.graphics.2d.batch :as batch]))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- draw-texture-region [batch texture-region [x y] [w h] rotation color]
  (if color (batch/set-color batch color))
  (batch/draw batch
              texture-region
              {:x x
               :y y
               :origin-x (/ (float w) 2) ; rotation origin
               :origin-y (/ (float h) 2)
               :width w
               :height h
               :scale-x 1
               :scale-y 1
               :rotation rotation})
  (if color (batch/set-color batch color/white)))

(defn draw-image
  [{:keys [clojure.context/unit-scale
           clojure.graphics/batch]}
   {:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image unit-scale)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [clojure.context/unit-scale
           clojure.graphics/batch]}
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
