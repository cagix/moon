(ns cdq.graphics.draws
  (:require [cdq.graphics.color :as color]
            [clojure.math :as math]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d Batch
                                          BitmapFont
                                          TextureRegion)
           (com.badlogic.gdx.utils Align)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- draw! [^BitmapFont font batch {:keys [scale text x y up? h-align target-width wrap?]}]
  (let [old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (* old-scale scale))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (or h-align Align/center)
           wrap?)
    (.setScale (.getData font) old-scale)))

(declare handle!)

(def ^:private draw-fns
  {:draw/with-line-width  (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]
                              :as graphics}
                             width
                             draws]
                            (let [old-line-width (.getDefaultLineWidth shape-drawer)]
                              (.setDefaultLineWidth shape-drawer (* width old-line-width))
                              (handle! graphics draws)
                              (.setDefaultLineWidth shape-drawer old-line-width)))

   :draw/grid             (fn do!
                            [graphics leftx bottomy gridw gridh cellw cellh color]
                            (let [w (* (float gridw) (float cellw))
                                  h (* (float gridh) (float cellh))
                                  topy (+ (float bottomy) (float h))
                                  rightx (+ (float leftx) (float w))]
                              (doseq [idx (range (inc (float gridw)))
                                      :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
                                (handle! graphics
                                         [[:draw/line [linex topy] [linex bottomy] color]]))
                              (doseq [idx (range (inc (float gridh)))
                                      :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
                                (handle! graphics
                                         [[:draw/line [leftx liney] [rightx liney] color]]))))
   :draw/texture-region   (fn
                            [{:keys [^Batch graphics/batch
                                     graphics/unit-scale
                                     graphics/world-unit-scale]}
                             ^TextureRegion texture-region
                             [x y]
                             & {:keys [center? rotation]}]
                            (let [[w h] (let [dimensions [(.getRegionWidth  texture-region)
                                                          (.getRegionHeight texture-region)]]
                                          (if (= @unit-scale 1)
                                            dimensions
                                            (mapv (comp float (partial * world-unit-scale))
                                                  dimensions)))]
                              (if center?
                                (.draw batch
                                       texture-region
                                       (- (float x) (/ (float w) 2))
                                       (- (float y) (/ (float h) 2))
                                       (/ (float w) 2) ; origin-x
                                       (/ (float h) 2) ; origin-y
                                       w
                                       h
                                       1 ; scale-x
                                       1 ; scale-y
                                       (or rotation 0))
                                (.draw batch
                                       texture-region
                                       (float x)
                                       (float y)
                                       (float w)
                                       (float h)))))
   :draw/text             (fn
                            [{:keys [graphics/batch
                                     graphics/unit-scale
                                     graphics/default-font]}
                             {:keys [font scale x y text h-align up?]}]
                            (draw! (or font default-font)
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
   :draw/ellipse          (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [x y] radius-x radius-y color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.ellipse shape-drawer x y radius-x radius-y))
   :draw/filled-ellipse   (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [x y] radius-x radius-y color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.filledEllipse shape-drawer x y radius-x radius-y))
   :draw/circle           (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [x y] radius color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.circle shape-drawer x y radius))
   :draw/filled-circle    (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [x y] radius color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.filledCircle shape-drawer (float x) (float y) (float radius)))
   :draw/rectangle        (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             x y w h color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.rectangle shape-drawer x y w h))
   :draw/filled-rectangle (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             x y w h color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)))
   :draw/arc              (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [center-x center-y] radius start-angle degree color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.arc shape-drawer
                                  center-x
                                  center-y
                                  radius
                                  (math/to-radians start-angle)
                                  (math/to-radians degree)))
   :draw/sector           (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [center-x center-y] radius start-angle degree color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.sector shape-drawer
                                     center-x
                                     center-y
                                     radius
                                     (math/to-radians start-angle)
                                     (math/to-radians degree)))
   :draw/line             (fn
                            [{:keys [^ShapeDrawer graphics/shape-drawer]}
                             [sx sy] [ex ey] color]
                            (.setColor shape-drawer (color/float-bits color))
                            (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))})

(defn handle! [graphics draws]
  (doseq [{k 0 :as component} draws
          :when component]
    (apply (draw-fns k) graphics (rest component))))
