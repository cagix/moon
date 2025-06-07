(ns gdl.create.graphics
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.texture.filter :as texture.filter]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.math.utils :as math-utils]
            [clojure.gdx.math.vector3 :as vector3]
            [clojure.gdx.utils.align :as align]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.graphics]
            [gdl.graphics.camera]
            [gdl.graphics.viewport]
            [gdl.tiled :as tiled]
            [gdl.utils.disposable :as disposable])
  (:import (com.badlogic.gdx.utils Disposable)
           (gdl.graphics OrthogonalTiledMapRenderer
                         ColorSetter)))

(defn- generate-font [file-handle params]
  (let [font (freetype/generate-font file-handle params)]
    (bitmap-font/configure! font params) ; DOTO ?
    font))

(defn- truetype-font [files {:keys [file size quality-scaling]}]
  (generate-font (files/internal files file)
                 {:size (* size quality-scaling)
                  :scale (/ quality-scaling)
                  :min-filter (texture.filter/->from-keyword :texture-filter/linear) ; because scaling to world-units
                  :mag-filter (texture.filter/->from-keyword :texture-filter/linear)
                  :enable-markup? true
                  :use-integer-positions? false}))  ; false, otherwise scaling to world-units not visible

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
  (if color (batch/set-color! batch (color/create :white))))

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

(defmulti draw! (fn [[k] _graphics]
                  k))

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
                             {:keys [batch
                                     unit-scale
                                     default-font]}]
  (let [font (or font default-font)
        scale (* (float @unit-scale)
                 (float (or scale 1)))
        old-scale (bitmap-font/scale-x font)]
    (bitmap-font/set-scale! font (* old-scale scale))
    (bitmap-font/draw! {:font font
                        :batch batch
                        :text text
                        :x x
                        :y (+ y (if up? (bitmap-font/text-height font text) 0))
                        :target-width 0
                        :align (align/->from-k (or h-align :center))
                        :wrap? false})
    (bitmap-font/set-scale! font old-scale)))

(defn- sd-set-color! [shape-drawer color]
  (sd/set-color! shape-drawer (color/create color)))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (math-utils/degree->radians start-angle)
           (math-utils/degree->radians degree)))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (math-utils/degree->radians start-angle)
              (math-utils/degree->radians degree)))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [shape-drawer]}]
  (sd-set-color! shape-drawer color)
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
      (gdl.graphics/handle-draws! this draws))))

(defrecord Graphics [graphics
                     textures
                     cursors
                     default-font
                     world-unit-scale
                     ui-viewport
                     world-viewport
                     batch
                     unit-scale
                     shape-drawer-texture
                     shape-drawer
                     tiled-map-renderer]
  disposable/Disposable
  (dispose! [_]
    (disposable/dispose! batch)
    (disposable/dispose! shape-drawer-texture)
    (run! disposable/dispose! (vals textures))
    (run! disposable/dispose! (vals cursors))
    (disposable/dispose! default-font))

  gdl.graphics/Graphics
  (resize-viewports! [_ width height]
    (gdl.graphics.viewport/resize! ui-viewport    width height)
    (gdl.graphics.viewport/resize! world-viewport width height))

  (delta-time [_]
    (graphics/delta-time graphics))

  (frames-per-second [_]
    (graphics/frames-per-second graphics))

  (set-cursor! [_ cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key)))

  (texture [_ path]
    (assert (contains? textures path) (str path))
    (get textures path))

  (draw-on-world-viewport! [_ f]
    (batch/set-color! batch (color/create :white)) ; fix scene2d.ui.tooltip flickering
    (batch/set-projection-matrix! batch (gdl.graphics.camera/combined (:camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (batch/end! batch))

  (draw-tiled-map! [_ tiled-map color-setter]
    (let [^OrthogonalTiledMapRenderer renderer (tiled-map-renderer tiled-map)
          camera (:camera world-viewport)]
      (.setColorSetter renderer (reify ColorSetter
                                  (apply [_ color x y]
                                    (color-setter color x y))))
      (.setView renderer (:camera/java-object camera))
      ; there is also:
      ; OrthogonalTiledMapRenderer/.renderTileLayer (TiledMapTileLayer layer)
      ; but right order / visible only ?
      (->> tiled-map
           tiled/layers
           (filter tiled/visible?)
           (map (partial tiled/layer-index tiled-map))
           int-array
           (.render renderer))))

  (handle-draws! [this draws]
    (doseq [component draws
            :when component]
      (draw! component this)))
  )

(defn- reify-camera [this]
  (reify
    clojure.lang.ILookup
    (valAt [_ k]
      (case k
        :camera/java-object this))

    gdl.graphics.camera/Camera
    (zoom [_]
      (:orthographic-camera/zoom this))

    (position [_]
      (:camera/position this))

    (combined [_]
      (:camera/combined this))

    (frustum [_]
      (let [frustum-points (take 4 (map vector3/->clj-vec (.planePoints  ; refl
                                                           (:camera/frustum this))))
            left-x   (apply min (map first  frustum-points))
            right-x  (apply max (map first  frustum-points))
            bottom-y (apply min (map second frustum-points))
            top-y    (apply max (map second frustum-points))]
        [left-x right-x bottom-y top-y]))

    (set-position! [_ position]
      (camera/set-position! this position))

    (set-zoom! [_ amount]
      (orthographic-camera/set-zoom! this amount))

    (viewport-width [_]
      (:camera/viewport-width this))

    (viewport-height [_]
      (:camera/viewport-height this))

    (reset-zoom! [cam]
      (orthographic-camera/set-zoom! this 1))

    (inc-zoom! [cam by]
      (orthographic-camera/set-zoom! this (max 0.1 (+ (:orthographic-camera/zoom this) by)))) ))

(defn- fit-viewport [width height camera {:keys [center-camera?]}]
  (let [this (fit-viewport/create width height camera)]
    (reify
      gdl.graphics.viewport/Viewport
      (resize! [_ width height]
        (viewport/update! this width height center-camera?))

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
          (viewport/unproject this clamped-x clamped-y)))

      clojure.lang.ILookup
      (valAt [_ key]
        (case key
          :java-object this
          :width  (.getWorldWidth  this)
          :height (.getWorldHeight this)
          :camera (reify-camera (.getCamera this)))))))

(defn- create-ui-viewport [{:keys [width height]}]
  (fit-viewport width
                height
                (orthographic-camera/create)
                {:center-camera? true}))

(defn- create-world-viewport [world-unit-scale {:keys [width height]}]
  (let [world-width  (* width world-unit-scale)
        world-height (* height world-unit-scale)]
    (fit-viewport world-width
                  world-height
                  (orthographic-camera/create {:world-width world-width
                                               :world-height world-height
                                               :y-down? false})
                  {:center-camera? false})))

(defn- white-pixel-texture []
  (let [pixmap (doto (pixmap/create 1 1)
                 (.setColor (color/create :white))
                 (.drawPixel 0 0))
        texture (texture/create pixmap)]
    (.dispose pixmap)
    texture))

(defn create-graphics [gdx-graphics
                       gdx-files
                       {:keys [textures
                               cursors ; optional
                               cursor-path-format ; optional
                               default-font ; optional, could use gdx included (BitmapFont.)
                               tile-size
                               ui-viewport
                               world-viewport]}]
  ;(println "load-textures (count textures): " (count textures))
  (let [batch (sprite-batch/create)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ tile-size))
        ui-viewport (create-ui-viewport ui-viewport)
        textures (into {} (for [file textures]
                            [file (texture/load! file)]))
        cursors (update-vals cursors
                             (fn [[file [hotspot-x hotspot-y]]]
                               (let [pixmap (pixmap/create (files/internal gdx-files (format cursor-path-format file)))
                                     cursor (graphics/cursor gdx-graphics pixmap hotspot-x hotspot-y)]
                                 (.dispose pixmap)
                                 cursor)))
        default-font (when default-font
                       (truetype-font gdx-files default-font))]
    (map->Graphics {:graphics gdx-graphics
                    :textures textures
                    :cursors cursors
                    :default-font default-font
                    :world-unit-scale world-unit-scale
                    :ui-viewport ui-viewport
                    :world-viewport (create-world-viewport world-unit-scale world-viewport)
                    :batch batch
                    :unit-scale (atom 1)
                    :shape-drawer-texture shape-drawer-texture
                    :shape-drawer (sd/create batch (texture-region/create shape-drawer-texture 1 0 1 1))
                    :tiled-map-renderer (memoize (fn [tiled-map]
                                                   (OrthogonalTiledMapRenderer. (:tiled-map/java-object tiled-map)
                                                                                (float world-unit-scale)
                                                                                batch)))})))
