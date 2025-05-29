(ns cdq.graphics
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.files :as files]
            [gdl.graphics :as graphics]
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport])
  (:import (com.badlogic.gdx.graphics Color
                                      Texture
                                      Pixmap
                                      Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.utils Disposable)))

(defprotocol PGraphics
  (delta-time [_])
  (set-cursor! [_ cursor])
  (frames-per-second [_])
  (clear-screen! [_])
  (draw-tiled-map! [_ tiled-map color-setter])
  (handle-draws! [_ draws])
  (draw-on-world-viewport! [_ f])
  (pixels->world-units [_ pixels])
  (sprite [_ texture])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite-sheet [x y]])
  (set-camera-position! [_ position])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (camera-position [_])
  (inc-zoom! [_ amount])
  (camera-frustum [_])
  (visible-tiles [_])
  (camera-zoom [_]))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- create-sprite [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defn- sub-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region
                  (int x)
                  (int y)
                  (int w)
                  (int h)))

(defmulti draw! (fn [[k] _this]
                  k))

(defrecord Graphics [graphics
                     ^SpriteBatch batch
                     unit-scale
                     world-unit-scale
                     shape-drawer-texture
                     shape-drawer
                     cursors
                     default-font
                     world-viewport
                     tiled-map-renderer]
  Disposable
  (dispose [_]
    (Disposable/.dispose batch)
    (Disposable/.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals cursors))
    (Disposable/.dispose default-font))

  PGraphics
  (delta-time [_]
    (graphics/delta-time graphics))

  (set-cursor! [_ cursor]
    (graphics/set-cursor! graphics (utils/safe-get cursors cursor)))

  (frames-per-second [_]
    (graphics/frames-per-second graphics))

  (clear-screen! [_]
    (graphics/clear-screen! graphics))

  (handle-draws! [this draws]
    (doseq [component draws
            :when component]
      (draw! component this)))

  (draw-tiled-map! [_ tiled-map color-setter]
    (tiled-map-renderer/draw! (tiled-map-renderer tiled-map)
                              tiled-map
                              color-setter
                              (:camera world-viewport)))

  (draw-on-world-viewport! [_ f]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (pixels->world-units [_ pixels]
    (* pixels world-unit-scale))

  (sprite [_ texture]
    (create-sprite (TextureRegion. ^Texture texture)
                   world-unit-scale))

  (sub-sprite [_ sprite [x y w h]]
    (create-sprite (sub-region (:texture-region sprite) x y w h)
                   world-unit-scale))

  (sprite-sheet [_ texture tilew tileh]
    {:image (create-sprite (TextureRegion. ^Texture texture)
                           world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
    (sub-sprite this image
                [(* x tilew)
                 (* y tileh)
                 tilew
                 tileh]))

  (set-camera-position! [_ position]
    (camera/set-position! (:camera world-viewport) position))

  (world-viewport-width [_]
    (:width world-viewport))

  (world-viewport-height [_]
    (:height world-viewport))

  (camera-position [_]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [_ amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [_]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [_]
    (camera/visible-tiles (:camera world-viewport)))

  (camera-zoom [_]
    (camera/zoom (:camera world-viewport))))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (let [font (freetype/generate (files/internal files file)
                                {:size (* size quality-scaling)})]
    (bitmap-font/configure! font {:scale (/ quality-scaling)
                                  :enable-markup? true
                                  :use-integer-positions? false}))) ; false, otherwise scaling to world-units not visible

(defn create [{:keys [ctx/config
                      ctx/files]
               :as ctx}]
  (map->Graphics
   (let [graphics (:clojure.gdx/graphics (:ctx/gdx ctx))
         {:keys [tile-size
                 cursor-path-format
                 cursors
                 default-font
                 world-viewport]} config
         batch (SpriteBatch.)
         shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                             (.setColor Color/WHITE)
                                             (.drawPixel 0 0))
                                    texture (Texture. pixmap)]
                                (.dispose pixmap)
                                texture)
         world-unit-scale (float (/ tile-size))]
     {:graphics graphics
      :batch batch
      :unit-scale (atom 1)
      :world-unit-scale world-unit-scale
      :shape-drawer-texture shape-drawer-texture
      :shape-drawer (sd/create batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
      :cursors (utils/mapvals
                (fn [[file [hotspot-x hotspot-y]]]
                  (let [pixmap (Pixmap. (files/internal files (format cursor-path-format file)))
                        cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                    (.dispose pixmap)
                    cursor))
                cursors)
      :default-font (truetype-font files default-font)
      :world-viewport (viewport/world-viewport world-unit-scale world-viewport)
      :tiled-map-renderer (memoize (fn [tiled-map]
                                     (tiled-map-renderer/create tiled-map world-unit-scale batch)))})))

(defn- draw-texture-region! [^SpriteBatch batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation)
  (if color (.setColor batch Color/WHITE)))

(defn- unit-dimensions [sprite unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions sprite)
    (:world-unit-dimensions sprite)))

(defn- draw-sprite!
  ([batch unit-scale {:keys [texture-region color] :as sprite} position]
   (draw-texture-region! batch
                         texture-region
                         position
                         (unit-dimensions sprite unit-scale)
                         0 ; rotation
                         color))
  ([batch unit-scale {:keys [texture-region color] :as sprite} [x y] rotation]
   (let [[w h] (unit-dimensions sprite unit-scale)]
     (draw-texture-region! batch
                           texture-region
                           [(- (float x) (/ (float w) 2))
                            (- (float y) (/ (float h) 2))]
                           [w h]
                           rotation
                           color))))


(defmethod draw! :draw/image [[_ sprite position]
                              {:keys [batch
                                      unit-scale]}]
  (draw-sprite! batch
                @unit-scale
                sprite
                position))

(defmethod draw! :draw/rotated-centered [[_ sprite rotation position]
                                         {:keys [batch
                                                 unit-scale]}]
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
                             {:keys [default-font
                                     batch
                                     unit-scale]}]
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
                                {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [shape-drawer]}]
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
                                        {:keys [shape-drawer] :as this}]
  (sd/with-line-width shape-drawer width
    (fn []
      (handle-draws! this draws))))
