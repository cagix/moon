(ns clojure.ctx.graphics
  (:require [clojure.graphics.color :as color]
            [clojure.graphics.batch :as batch]
            [clojure.graphics.texture :as texture]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.graphics.g2d.bitmap-font :as bitmap-font]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn- draw-texture-region! [batch texture-region [x y] [w h] rotation color]
  (if color (batch/set-color! batch color))
  (batch/draw! batch
               texture-region
               {:x x
                :y y
                :origin-x (/ (float w) 2)
                :origin-y (/ (float h) 2)
                :width w
                :height h
                :scale-x 1
                :scale-y 1
                :rotation rotation})
  (if color (batch/set-color! batch color/white)))

(defn- unit-dimensions [sprite unit-scale]
  (if (= unit-scale 1)
    (:sprite/pixel-dimensions sprite)
    (:sprite/world-unit-dimensions sprite)))

(defn- draw-sprite!
  ([batch unit-scale {:keys [sprite/texture-region sprite/color] :as sprite} position]
   (draw-texture-region! batch
                         texture-region
                         position
                         (unit-dimensions sprite unit-scale)
                         0 ; rotation
                         color))
  ([batch unit-scale {:keys [sprite/texture-region sprite/color] :as sprite} [x y] rotation]
   (let [[w h] (unit-dimensions sprite unit-scale)]
     (draw-texture-region! batch
                           texture-region
                           [(- (float x) (/ (float w) 2))
                            (- (float y) (/ (float h) 2))]
                           [w h]
                           rotation
                           color))))

(defmulti draw! (fn [[k] _this]
                  k))

(defn handle-draws! [this draws] ; batch, unit-scale, default-font, shape-drawer.
  (doseq [component draws
          :when component]
    (draw! component this)))

(defmethod draw! :draw/image [[_ sprite position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (draw-sprite! batch
                @unit-scale
                sprite
                position))

(defmethod draw! :draw/rotated-centered [[_ sprite rotation position]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (draw-sprite! batch
                @unit-scale
                sprite
                position
                rotation))

(defmethod draw! :draw/centered [[_ image position] this]
  (draw! [:draw/rotated-centered image 0 position] this))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [ctx/default-font
                                     ctx/batch
                                     ctx/unit-scale]}]
  (bitmap-font/draw! (or font default-font)
                     batch
                     {:scale (* (float @unit-scale)
                                (float (or scale 1)))
                      :x x
                      :y y
                      :text text
                      :h-align h-align
                      :up? up?}))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] this]
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
                                        {:keys [ctx/shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (handle-draws! this draws))))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(q/defrecord Sprite [sprite/texture-region
                     sprite/pixel-dimensions
                     sprite/world-unit-dimensions
                     sprite/color]) ; optional

(defn- create-sprite
  [^TextureRegion texture-region
   world-unit-scale]
  (let [scale 1 ; "scale can be a number for multiplying the texture-region-dimensions or [w h]."
        _ (assert (or (number? scale)
                      (and (vector? scale)
                           (number? (scale 0))
                           (number? (scale 1)))))
        pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (map->Sprite {:texture-region texture-region
                  :pixel-dimensions pixel-dimensions
                  :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale)})))

(defn sprite [{:keys [ctx/assets
                      ctx/world-unit-scale]}
              texture-path]
  (create-sprite (texture/region (assets texture-path))
                 world-unit-scale))

(defn sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
  (create-sprite (texture/sub-region (:sprite/texture-region sprite) x y w h)
                 world-unit-scale))

(defn sprite-sheet [{:keys [ctx/assets
                            ctx/world-unit-scale]}
                    texture-path
                    tilew
                    tileh]
  {:image (create-sprite (texture/region (assets texture-path))
                         world-unit-scale)
   :tilew tilew
   :tileh tileh})

(defn sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
  (sub-sprite this image
              [(* x tilew)
               (* y tileh)
               tilew
               tileh]))
