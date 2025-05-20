(ns cdq.draw
  (:require [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.shape-drawer :as sd]))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn image [{:keys [ctx/batch
                     ctx/unit-scale]}
             {:keys [texture-region color] :as image}
             position]
  (batch/draw-texture-region! batch
                              texture-region
                              position
                              (unit-dimensions image @unit-scale)
                              0 ; rotation
                              color))

(defn rotated-centered [{:keys [ctx/batch
                                ctx/unit-scale]}
                        {:keys [texture-region color] :as image}
                        rotation
                        [x y]]
  (let [[w h] (unit-dimensions image @unit-scale)]
    (batch/draw-texture-region! batch
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
  [{:keys [ctx/default-font
           ctx/batch
           ctx/unit-scale]}
   {:keys [font scale x y text h-align up?]}]
  (graphics/draw-text! (or font default-font)
                       batch
                       {:scale (* (float @unit-scale)
                                  (float (or scale 1)))
                        :x x
                        :y y
                        :text text
                        :h-align h-align
                        :up? up?}))

(defn ellipse [{:keys [ctx/shape-drawer]} [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defn filled-ellipse [{:keys [ctx/shape-drawer]} [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defn circle [{:keys [ctx/shape-drawer]} [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defn filled-circle [{:keys [ctx/shape-drawer]} [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defn arc [{:keys [ctx/shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defn sector [{:keys [ctx/shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defn rectangle [{:keys [ctx/shape-drawer]} x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defn filled-rectangle [{:keys [ctx/shape-drawer]} x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defn line [{:keys [ctx/shape-drawer]} [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defn with-line-width [{:keys [ctx/shape-drawer]} width draw-fn]
  (sd/with-line-width shape-drawer width draw-fn))

(defn grid [draw leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line draw [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line draw [leftx liney] [rightx liney] color))))

(defn centered [draw image position]
  (rotated-centered draw image 0 position))
