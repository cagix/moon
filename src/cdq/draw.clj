(ns cdq.draw
  (:require [cdq.ctx :as ctx]
            [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.shape-drawer :as sd]))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn image [{:keys [texture-region color] :as image} position]
  (batch/draw-texture-region! ctx/batch
                              texture-region
                              position
                              (unit-dimensions image @ctx/unit-scale)
                              0 ; rotation
                              color))

(defn rotated-centered [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image @ctx/unit-scale)]
    (batch/draw-texture-region! ctx/batch
                                texture-region
                                [(- (float x) (/ (float w) 2))
                                 (- (float y) (/ (float h) 2))]
                                [w h]
                                rotation
                                color)))

(defn text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font scale x y text h-align up?]}]
  (graphics/draw-text! (or font ctx/default-font)
                       ctx/batch
                       {:scale (* (float @ctx/unit-scale)
                                  (float (or scale 1)))
                        :x x
                        :y y
                        :text text
                        :h-align h-align
                        :up? up?}))

(defn ellipse [[x y] radius-x radius-y color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/ellipse! ctx/shape-drawer x y radius-x radius-y))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/filled-ellipse! ctx/shape-drawer x y radius-x radius-y))

(defn circle [[x y] radius color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/circle! ctx/shape-drawer x y radius))

(defn filled-circle [[x y] radius color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/filled-circle! ctx/shape-drawer x y radius))

(defn arc [[center-x center-y] radius start-angle degree color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/arc! ctx/shape-drawer center-x center-y radius start-angle degree))

(defn sector [[center-x center-y] radius start-angle degree color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/sector! ctx/shape-drawer center-x center-y radius start-angle degree))

(defn rectangle [x y w h color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/rectangle! ctx/shape-drawer x y w h))

(defn filled-rectangle [x y w h color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/filled-rectangle! ctx/shape-drawer x y w h))

(defn line [[sx sy] [ex ey] color]
  (sd/set-color! ctx/shape-drawer color)
  (sd/line! ctx/shape-drawer sx sy ex ey))

(defn with-line-width [width draw-fn]
  (sd/with-line-width ctx/shape-drawer width draw-fn))

(defn grid [leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line [leftx liney] [rightx liney] color))))

(defn centered [image position]
  (rotated-centered image 0 position))
