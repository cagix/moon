(ns gdl.graphics
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [gdx.graphics.shape-drawer :as sd])
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defprotocol Graphics
  (clear-screen! [_ color])
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor-key])
  (texture [_ path])
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (resize-viewports! [_ width height])
  (ui-viewport-height [_])
  (image->texture-region [_ image]
                         "image is `:image/file` (string) & `:image/bounds` `[x y w h]` (optional).

                         Loads the texture and creates a texture-region out of it, in case of sub-image bounds applies the proper bounds."))

(defmulti ^:private draw!
  (fn [[k] _graphics]
    k))

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))

(defn- texture-region-drawing-dimensions
  [{:keys [unit-scale
           world-unit-scale]}
   texture-region]
  (let [dimensions (texture-region/dimensions texture-region)]
    (if (= @unit-scale 1)
      dimensions
      (mapv (comp float (partial * world-unit-scale))
            dimensions))))

(defn- batch-draw! [^Batch batch texture-region [x y] [w h] rotation]
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

(defmethod draw! :draw/texture-region [[_ texture-region [x y]]
                                       {:keys [batch]}]
  (batch-draw! batch
               texture-region
               [x y]
               (texture-region/dimensions texture-region)
               0))

(defmethod draw! :draw/image [[_ image position]
                              {:keys [batch]
                               :as graphics}]
  (let [texture-region (image->texture-region graphics image)]
    (batch-draw! batch
                 texture-region
                 position
                 (texture-region-drawing-dimensions graphics texture-region)
                 0)))

(defmethod draw! :draw/rotated-centered [[_ image rotation [x y]]
                                         {:keys [batch]
                                          :as graphics}]
  (let [texture-region (image->texture-region graphics image)
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

(defmethod draw! :draw/ellipse [[_ position radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd/ellipse! shape-drawer position radius-x radius-y color))

(defmethod draw! :draw/filled-ellipse [[_ position radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd/filled-ellipse! shape-drawer position radius-x radius-y color))

(defmethod draw! :draw/circle [[_ position radius color]
                               {:keys [shape-drawer]}]
  (sd/circle! shape-drawer position radius color))

(defmethod draw! :draw/filled-circle [[_ position radius color]
                                      {:keys [shape-drawer]}]
  (sd/filled-circle! shape-drawer position radius color))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd/rectangle! shape-drawer x y w h color))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd/filled-rectangle! shape-drawer x y w h color))

(defmethod draw! :draw/arc [[_ center-position radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd/arc! shape-drawer center-position radius start-angle degree color))

(defmethod draw! :draw/sector [[_ center-position radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd/sector! shape-drawer center-position radius start-angle degree color))

(defmethod draw! :draw/line [[_ start end color]
                             {:keys [shape-drawer]}]
  (sd/line! shape-drawer start end color))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color]
                             {:keys [shape-drawer]}]
  (sd/grid! shape-drawer leftx bottomy gridw gridh cellw cellh color))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (handle-draws! this draws))))
