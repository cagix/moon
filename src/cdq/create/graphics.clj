(ns cdq.create.graphics
  (:require [cdq.g :as g]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.application]
            [gdl.graphics :as graphics]
            [gdl.tiled :as tiled]
            [gdl.utils :as utils]))

(defn add [ctx config]
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))]
    (merge ctx
           {:ctx/batch batch
            :ctx/unit-scale 1
            :ctx/world-unit-scale world-unit-scale
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create (:java-object batch)
                                         (graphics/texture-region shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals
                          (fn [[file [hotspot-x hotspot-y]]]
                            (graphics/create-cursor (format (:cursor-path-format config) file)
                                                    hotspot-x
                                                    hotspot-y))
                          (:cursors config))
            :ctx/default-font (graphics/truetype-font (:default-font config))
            :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                               (tiled/renderer tiled-map
                                                               world-unit-scale
                                                               (:java-object batch))))})))

(extend-type gdl.application.Context
  g/TiledMapRenderer
  (draw-tiled-map! [{:keys [ctx/world-viewport
                            ctx/tiled-map-renderer]}
                    tiled-map
                    color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport))))

(extend-type gdl.application.Context
  g/Graphics
  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/shape-drawer
                                    ctx/world-unit-scale]
                             :as ctx}
                            fns]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (doseq [f fns]
                                        (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale))

  (sprite [{:keys [ctx/world-unit-scale] :as ctx} texture-path]
    (graphics/sprite (graphics/texture-region (g/texture ctx texture-path))
                     world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
    (graphics/sprite (graphics/sub-region (:texture-region sprite) x y w h)
                     world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale] :as ctx} texture-path tilew tileh]
    {:image (graphics/sprite (graphics/texture-region (g/texture ctx texture-path))
                             world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [ctx {:keys [image tilew tileh]} [x y]]
    (g/sub-sprite ctx image [(* x tilew) (* y tileh) tilew tileh])))

(defmulti draw! (fn [[k] _ctx]
                  k))

(extend-type gdl.application.Context
  g/Draws
  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx))))

(defmethod draw! :draw/image [[_ sprite position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (graphics/draw-sprite! batch
                         unit-scale
                         sprite
                         position))

(defmethod draw! :draw/rotated-centered [[_ sprite rotation position]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (graphics/draw-sprite! batch
                         unit-scale
                         sprite
                         position
                         rotation))

(defmethod draw! :draw/centered [[_ image position] ctx]
  (draw! [:draw/rotated-centered image 0 position] ctx))

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
                     {:scale (* (float unit-scale)
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

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] ctx]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] ctx))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] ctx))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [ctx/shape-drawer]
                                         :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (g/handle-draws! ctx draws))))
