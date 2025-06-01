(ns cdq.create.graphics
  (:require [cdq.application]
            [cdq.graphics :as g]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [gdl.assets :as assets]
            [gdl.files :as files]
            [gdl.graphics :as graphics]
            [gdl.graphics.texture :as texture]
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.utils :as utils])
  (:import (cdq.application Context)
           (com.badlogic.gdx.graphics Pixmap
                                      Texture$TextureFilter)))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (let [font (freetype/generate (files/internal files file)
                                {:size (* size quality-scaling)
                                 :min-filter Texture$TextureFilter/Linear ; because scaling to world-units
                                 :mag-filter Texture$TextureFilter/Linear})]
    (bitmap-font/configure! font {:scale (/ quality-scaling)
                                  :enable-markup? true
                                  :use-integer-positions? false}))) ; false, otherwise scaling to world-units not visible

(defmulti ^:private draw! (fn [[k] _this]
                            k))

(defmethod draw! :draw/image [[_ sprite position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (graphics/draw-sprite! batch
                         @unit-scale
                         sprite
                         position))

(defmethod draw! :draw/rotated-centered [[_ sprite rotation position]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (graphics/draw-sprite! batch
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
      (g/handle-draws! this draws))))

(defn do! [{:keys [ctx/config
                   ctx/files
                   ctx/graphics]
            :as ctx}]
  (merge ctx
         (let [{:keys [tile-size
                       cursor-path-format
                       cursors
                       default-font]} config
               batch (graphics/sprite-batch)
               shape-drawer-texture (graphics/white-pixel-texture)
               world-unit-scale (float (/ tile-size))]
           {:ctx/ui-viewport (graphics/ui-viewport (:ui-viewport config))
            :ctx/batch batch
            :ctx/unit-scale (atom 1)
            :ctx/world-unit-scale world-unit-scale
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create batch (texture/->sub-region shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals (fn [[file [hotspot-x hotspot-y]]]
                                          (let [pixmap (Pixmap. (files/internal files (format cursor-path-format file)))
                                                cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                                            (.dispose pixmap)
                                            cursor))
                                        cursors)
            :ctx/default-font (truetype-font files default-font)
            :ctx/world-viewport (graphics/world-viewport world-unit-scale (:world-viewport config))
            :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                               (tiled-map-renderer/create tiled-map world-unit-scale batch)))})))

(extend-type Context
  g/Graphics
  (handle-draws! [this draws]
    (doseq [component draws
            :when component]
      (draw! component this)))

  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/shape-drawer
                                    ctx/world-unit-scale
                                    ctx/unit-scale]}
                            f]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (reset! unit-scale world-unit-scale)
                                      (f)
                                      (reset! unit-scale 1))))))

  (sprite [{:keys [ctx/assets
                   ctx/world-unit-scale]}
           texture-path]
    (graphics/create-sprite (texture/region (assets/texture assets texture-path))
                            world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
    (graphics/create-sprite (texture/sub-region (:texture-region sprite) x y w h)
                            world-unit-scale))

  (sprite-sheet [{:keys [ctx/assets
                         ctx/world-unit-scale]}
                 texture-path
                 tilew
                 tileh]
    {:image (graphics/create-sprite (texture/region (assets/texture assets texture-path))
                                    world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
    (g/sub-sprite this image
                  [(* x tilew)
                   (* y tileh)
                   tilew
                   tileh]))

  (set-camera-position! [{:keys [ctx/world-viewport]} position]
    (camera/set-position! (:camera world-viewport) position))

  (world-viewport-width [{:keys [ctx/world-viewport]}]
    (:width world-viewport))

  (world-viewport-height [{:keys [ctx/world-viewport]}]
    (:height world-viewport))

  (camera-position [{:keys [ctx/world-viewport]}]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [{:keys [ctx/world-viewport]} amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [{:keys [ctx/world-viewport]}]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [{:keys [ctx/world-viewport]}]
    (camera/visible-tiles (:camera world-viewport)))

  (camera-zoom [{:keys [ctx/world-viewport]}]
    (camera/zoom (:camera world-viewport)))

  (draw-tiled-map! [{:keys [ctx/tiled-map-renderer
                            ctx/world-viewport]}
                    tiled-map
                    color-setter]
    (tiled-map-renderer/draw! (tiled-map-renderer tiled-map)
                              tiled-map
                              color-setter
                              (:camera world-viewport)))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale)))
