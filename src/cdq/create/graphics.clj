(ns cdq.create.graphics
  (:require [cdq.assets :as assets]
            [cdq.g :as g]
            [cdq.input :as input]
            [cdq.application :as application]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.math.math-utils :as math-utils]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport])
  (:import (cdq.application Context)
           (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color
                                      Texture
                                      Texture$TextureFilter
                                      Pixmap
                                      Pixmap$Format
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport FitViewport)))

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

(defmulti ^:private draw! (fn [[k] _this]
                            k))

(defn- truetype-font [{:keys [file size quality-scaling]}]
  (let [font (freetype/generate (.internal Gdx/files file)
                                {:size (* size quality-scaling)
                                 :min-filter Texture$TextureFilter/Linear ; because scaling to world-units
                                 :mag-filter Texture$TextureFilter/Linear})]
    (bitmap-font/configure! font {:scale (/ quality-scaling)
                                  :enable-markup? true
                                  :use-integer-positions? false}))) ; false, otherwise scaling to world-units not visible

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
      (g/handle-draws! this draws))))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (FitViewport. width height camera)]
    (reify
      application/Resizable
      (resize! [_ width height]
        (.update this width height center-camera?))

      viewport/Viewport
      ; touch coordinates are y-down, while screen coordinates are y-up
      ; so the clamping of y is reverse, but as black bars are equal it does not matter
      ; TODO clamping only works for gui-viewport ?
      ; TODO ? "Can be negative coordinates, undefined cells."
      (unproject [_ [x y]]
        (let [clamped-x (math-utils/clamp x
                                          (.getLeftGutterWidth this)
                                          (.getRightGutterX    this))
              clamped-y (math-utils/clamp y
                                          (.getTopGutterHeight this)
                                          (.getTopGutterY      this))]
          (let [v2 (.unproject this (Vector2. clamped-x clamped-y))]
            [(.x v2) (.y v2)])))

      ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (.getCamera      this))))))

(defn- ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (OrthographicCamera.)
                {:center-camera? true}))

(defn- world-viewport [world-unit-scale {:keys [width height]}]
  (let [camera (OrthographicCamera.)
        world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width
                  world-height
                  camera
                  {:center-camera? false})))

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (merge ctx
         (let [graphics Gdx/graphics
               {:keys [tile-size
                       cursor-path-format
                       cursors
                       default-font]} config
               batch (SpriteBatch.)
               shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                                   (.setColor Color/WHITE)
                                                   (.drawPixel 0 0))
                                          texture (Texture. pixmap)]
                                      (.dispose pixmap)
                                      texture)
               world-unit-scale (float (/ tile-size))]
           {:ctx/ui-viewport (ui-viewport (:ui-viewport config))
            :ctx/graphics graphics
            :ctx/batch batch
            :ctx/unit-scale (atom 1)
            :ctx/world-unit-scale world-unit-scale
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals
                          (fn [[file [hotspot-x hotspot-y]]]
                            (let [pixmap (Pixmap. (.internal Gdx/files (format cursor-path-format file)))
                                  cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
                              (.dispose pixmap)
                              cursor))
                          cursors)
            :ctx/default-font (truetype-font default-font)
            :ctx/world-viewport (world-viewport world-unit-scale (:world-viewport config))
            :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                               (tiled-map-renderer/create tiled-map world-unit-scale batch)))})))

(extend-type Context
  g/MouseViewports
  (world-mouse-position [{:keys [ctx/world-viewport] :as ctx}]
    (viewport/unproject world-viewport (input/mouse-position ctx)))

  (ui-mouse-position [{:keys [ctx/ui-viewport] :as ctx}]
    (viewport/unproject ui-viewport (input/mouse-position ctx))))

(extend-type Context
  g/Graphics
  (delta-time [{:keys [ctx/graphics]}]
    (.getDeltaTime graphics))

  (frames-per-second [{:keys [ctx/graphics]}]
    (.getFramesPerSecond graphics))

  (clear-screen! [_]
    (ScreenUtils/clear Color/BLACK))

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
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (sprite [{:keys [ctx/world-unit-scale] :as ctx} texture-path]
    (create-sprite (TextureRegion. ^Texture (assets/texture ctx texture-path))
                   world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
    (create-sprite (sub-region (:texture-region sprite) x y w h)
                   world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale]
                  :as ctx}
                 texture-path
                 tilew
                 tileh]
    {:image (create-sprite (TextureRegion. ^Texture (assets/texture ctx texture-path))
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

  (set-cursor! [{:keys [ctx/graphics
                        ctx/cursors]}
                cursor]
    (.setCursor graphics (utils/safe-get cursors cursor)))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale)))
