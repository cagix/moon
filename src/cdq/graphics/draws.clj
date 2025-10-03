(ns cdq.graphics.draws
  (:require [cdq.graphics :as graphics]
            [space.earlygrey.shape-drawer :as sd]
            [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.g2d.batch :as batch]
            [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.math :refer [degree->radians]]))

(def ^:private draw-fns
  {:draw/with-line-width  (fn [{:keys [graphics/shape-drawer]
                                :as graphics}
                               width
                               draws]
                            (sd/with-line-width shape-drawer width
                              (graphics/handle-draws! graphics draws)))
   :draw/grid             (fn
                            [graphics leftx bottomy gridw gridh cellw cellh color]
                            (let [w (* (float gridw) (float cellw))
                                  h (* (float gridh) (float cellh))
                                  topy (+ (float bottomy) (float h))
                                  rightx (+ (float leftx) (float w))]
                              (doseq [idx (range (inc (float gridw)))
                                      :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
                                (graphics/handle-draws! graphics
                                                        [[:draw/line [linex topy] [linex bottomy] color]]))
                              (doseq [idx (range (inc (float gridh)))
                                      :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
                                (graphics/handle-draws! graphics
                                                        [[:draw/line [leftx liney] [rightx liney] color]]))))
   :draw/texture-region   (fn [{:keys [graphics/batch
                                       graphics/unit-scale
                                       graphics/world-unit-scale]}
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
   :draw/text             (fn [{:keys [graphics/batch
                                       graphics/unit-scale
                                       graphics/default-font]}
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
   :draw/ellipse          (fn [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/ellipse! shape-drawer x y radius-x radius-y))
   :draw/filled-ellipse   (fn [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
   :draw/circle           (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/circle! shape-drawer x y radius))
   :draw/filled-circle    (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/filled-circle! shape-drawer x y radius))
   :draw/rectangle        (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/rectangle! shape-drawer x y w h))
   :draw/filled-rectangle (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/filled-rectangle! shape-drawer x y w h))
   :draw/arc              (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/arc! shape-drawer
                                     center-x
                                     center-y
                                     radius
                                     (degree->radians start-angle)
                                     (degree->radians degree)))
   :draw/sector           (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/sector! shape-drawer
                                        center-x
                                        center-y
                                        radius
                                        (degree->radians start-angle)
                                        (degree->radians degree)))
   :draw/line             (fn [{:keys [graphics/shape-drawer]} [sx sy] [ex ey] color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/line! shape-drawer sx sy ex ey))})

(defn create [graphics]
  (extend-type (class graphics)
    graphics/Draws
    (handle-draws! [graphics draws]
      (doseq [{k 0 :as component} draws
              :when component]
        (apply (draw-fns k) graphics (rest component)))))
  graphics)
