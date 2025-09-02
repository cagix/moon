(ns cdq.draws-impl
  (:require [cdq.gdx.graphics.color :as color]
            [cdq.graphics-impl :refer [draw!]]
            [cdq.ctx.graphics]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.utils Align)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn- degree->radians [degree]
  (* degrees-to-radians (float degree)))

(def ^:private align-k->value
  {:bottom       Align/bottom
   :bottom-left  Align/bottomLeft
   :bottom-right Align/bottomRight
   :center       Align/center
   :left         Align/left
   :right        Align/right
   :top          Align/top
   :top-left     Align/topLeft
   :top-right    Align/topRight})

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- bitmap-font-draw!
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [^BitmapFont font
   batch
   {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align-k->value h-align))]}
  (let [old-scale (.scaleX (.getData font))]
    (.setScale (.getData font) (float (* old-scale scale)))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align-k->value (or h-align :center))
           wrap?)
    (.setScale (.getData font) (float old-scale))))

(defn- texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   ^TextureRegion texture-region]
  (let [dimensions [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defn- batch-draw! [^SpriteBatch batch texture-region [x y] [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defmethod draw! :draw/texture-region [[_ ^TextureRegion texture-region [x y]]
                                       {:keys [batch]}]
  (batch-draw! batch
               texture-region
               [x y]
               [(.getRegionWidth  texture-region)
                (.getRegionHeight texture-region)]
               0))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (cdq.ctx.graphics/image->texture-region graphics image)]
    (batch-draw! batch
                 texture-region
                 position
                 (texture-region-drawing-dimensions graphics texture-region)
                 0)))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (cdq.ctx.graphics/image->texture-region graphics image)
        [w h] (texture-region-drawing-dimensions graphics texture-region)]
    (batch-draw! batch
                 texture-region
                 [(- (float x) (/ (float w) 2))
                  (- (float y) (/ (float h) 2))]
                 [w h]
                 rotation)))

(defmethod draw! :draw/centered [[_ image position] this]
  (draw! [:draw/rotated-centered image 0 position] this))

(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [batch
                                     unit-scale
                                     default-font]}]
  (bitmap-font-draw! (or font default-font)
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

(defn- sd-set-color! [shape-drawer color]
  (ShapeDrawer/.setColor shape-drawer (color/->obj color)))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.ellipse shape-drawer
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.filledEllipse shape-drawer
                              (float x)
                              (float y)
                              (float radius-x)
                              (float radius-y)))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.circle shape-drawer
                       (float x)
                       (float y)
                       (float radius)))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.filledCircle shape-drawer
                             (float x)
                             (float y)
                             (float radius)))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.rectangle shape-drawer
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.filledRectangle shape-drawer
                                (float x)
                                (float y)
                                (float w)
                                (float h)))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.arc shape-drawer
                    (float center-x)
                    (float center-y)
                    (float radius)
                    (float (degree->radians start-angle))
                    (float (degree->radians degree))))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.sector shape-drawer
                       (float center-x)
                       (float center-y)
                       (float radius)
                       (float (degree->radians start-angle))
                       (float (degree->radians degree))))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (ShapeDrawer/.line shape-drawer
                     (float sx)
                     (float sy)
                     (float ex)
                     (float ey)))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color]
                             this]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] this))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] this))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (cdq.graphics-impl/sd-with-line-width shape-drawer width
                                        (fn []
                                          (cdq.ctx.graphics/handle-draws! this draws))))
