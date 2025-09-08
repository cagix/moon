(ns cdq.graphics
  (:require [cdq.math :as math]
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]))

(defn draw-on-world-viewport!
  [{:keys [ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]}
   draw!]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (draw!)
      (reset! unit-scale 1)))
  (batch/end! batch))

(declare handle-draws!)

(def ^:private draw-fns
  {:draw/with-line-width (fn [[_ width draws]
                            {:keys [ctx/shape-drawer] :as ctx}]
                           (sd/with-line-width shape-drawer width
                             (fn []
                               (handle-draws! ctx draws))))
   :draw/grid (fn [[_ leftx bottomy gridw gridh cellw cellh color]
                   ctx]
                (let [w (* (float gridw) (float cellw))
                      h (* (float gridh) (float cellh))
                      topy (+ (float bottomy) (float h))
                      rightx (+ (float leftx) (float w))]
                  (doseq [idx (range (inc (float gridw)))
                          :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
                    (handle-draws! [[:draw/line [linex topy] [linex bottomy] color]]
                                   ctx))
                  (doseq [idx (range (inc (float gridh)))
                          :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
                    (handle-draws! [[:draw/line [leftx liney] [rightx liney] color]]
                                   ctx))))
   :draw/texture-region (fn [[_ texture-region [x y] {:keys [center? rotation]}]
                             {:keys [ctx/batch
                                     ctx/unit-scale
                                     ctx/world-unit-scale]}]
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
   :draw/text (fn [[_ {:keys [font scale x y text h-align up?]}]
                 {:keys [ctx/batch
                         ctx/unit-scale
                         ctx/default-font]}]
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
   :draw/ellipse (fn [[_ [x y] radius-x radius-y color]
                      {:keys [ctx/shape-drawer]}]
                   (sd/set-color! shape-drawer (color/->obj color))
                   (sd/ellipse! shape-drawer x y radius-x radius-y))
   :draw/filled-ellipse (fn [[_ [x y] radius-x radius-y color]
                             {:keys [ctx/shape-drawer]}]
                          (sd/set-color! shape-drawer (color/->obj color))
                          (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
   :draw/circle (fn [[_ [x y] radius color]
                     {:keys [ctx/shape-drawer]}]
                  (sd/set-color! shape-drawer (color/->obj color))
                  (sd/circle! shape-drawer x y radius))
   :draw/filled-circle (fn [[_ [x y] radius color]
                            {:keys [ctx/shape-drawer]}]
                         (sd/set-color! shape-drawer (color/->obj color))
                         (sd/filled-circle! shape-drawer x y radius))
   :draw/rectangle (fn [[_ x y w h color]
                        {:keys [ctx/shape-drawer]}]
                     (sd/set-color! shape-drawer (color/->obj color))
                     (sd/rectangle! shape-drawer x y w h))
   :draw/filled-rectangle (fn [[_ x y w h color]
                               {:keys [ctx/shape-drawer]}]
                            (sd/set-color! shape-drawer (color/->obj color))
                            (sd/filled-rectangle! shape-drawer x y w h))
   :draw/arc (fn [[_ [center-x center-y] radius start-angle degree color]
                  {:keys [ctx/shape-drawer]}]
               (sd/set-color! shape-drawer (color/->obj color))
               (sd/arc! shape-drawer
                        center-x
                        center-y
                        radius
                        (math/degree->radians start-angle)
                        (math/degree->radians degree)))
   :draw/sector (fn [[_ [center-x center-y] radius start-angle degree color]
                     {:keys [ctx/shape-drawer]}]
                  (sd/set-color! shape-drawer (color/->obj color))
                  (sd/sector! shape-drawer
                              center-x
                              center-y
                              radius
                              (math/degree->radians start-angle)
                              (math/degree->radians degree)))
   :draw/line (fn [[_ [sx sy] [ex ey] color]
                   {:keys [ctx/shape-drawer]}]
                (sd/set-color! shape-drawer (color/->obj color))
                (sd/line! shape-drawer sx sy ex ey))})

(defn- draw!
  [{k 0 :as component} ctx]
  ((draw-fns k) component ctx))

(defn handle-draws! [ctx draws]
  (doseq [component draws
          :when component]
    (draw! component ctx)))
