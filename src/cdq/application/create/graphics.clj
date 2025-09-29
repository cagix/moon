(ns cdq.application.create.graphics
  (:require [cdq.graphics]
            [clojure.disposable :as disposable]
            [clojure.files.utils :as files-utils]
            [clojure.files :as files]
            [clojure.gdx :as gdx]
            [clojure.graphics :as graphics]
            [clojure.graphics.batch :as batch]
            [clojure.graphics.bitmap-font :as bitmap-font]
            [clojure.graphics.color :as color]
            [clojure.graphics.orthographic-camera :as camera]
            [space.earlygrey.shape-drawer :as sd]
            [clojure.graphics.pixmap :as pixmap]
            [clojure.graphics.texture :as texture]
            [clojure.graphics.texture-region :as texture-region]
            [clojure.graphics.viewport :as viewport]
            [com.badlogic.gdx.graphics.colors :as colors]
            [com.badlogic.gdx.graphics.g2d.freetype :as freetype]
            [gdl.math :as math]
            [gdl.maps.tiled.renderers.orthogonal :as tm-renderer]))

(def ^:private draw-fns
  {:draw/with-line-width  (fn [{:keys [graphics/shape-drawer]
                                :as graphics}
                               width
                               draws]
                            (sd/with-line-width shape-drawer width
                              (fn []
                                (cdq.graphics/handle-draws! graphics draws))))
   :draw/grid             (fn
                            [graphics leftx bottomy gridw gridh cellw cellh color]
                            (let [w (* (float gridw) (float cellw))
                                  h (* (float gridh) (float cellh))
                                  topy (+ (float bottomy) (float h))
                                  rightx (+ (float leftx) (float w))]
                              (doseq [idx (range (inc (float gridw)))
                                      :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
                                (cdq.graphics/handle-draws! graphics
                                                            [[:draw/line [linex topy] [linex bottomy] color]]))
                              (doseq [idx (range (inc (float gridh)))
                                      :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
                                (cdq.graphics/handle-draws! graphics
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
                            (sd/set-color! shape-drawer color)
                            (sd/ellipse! shape-drawer x y radius-x radius-y))
   :draw/filled-ellipse   (fn [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
                            (sd/set-color! shape-drawer color)
                            (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
   :draw/circle           (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/set-color! shape-drawer color)
                            (sd/circle! shape-drawer x y radius))
   :draw/filled-circle    (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/set-color! shape-drawer color)
                            (sd/filled-circle! shape-drawer x y radius))
   :draw/rectangle        (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/set-color! shape-drawer color)
                            (sd/rectangle! shape-drawer x y w h))
   :draw/filled-rectangle (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/set-color! shape-drawer color)
                            (sd/filled-rectangle! shape-drawer x y w h))
   :draw/arc              (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/set-color! shape-drawer color)
                            (sd/arc! shape-drawer
                                     center-x
                                     center-y
                                     radius
                                     (math/degree->radians start-angle)
                                     (math/degree->radians degree)))
   :draw/sector           (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/set-color! shape-drawer color)
                            (sd/sector! shape-drawer
                                        center-x
                                        center-y
                                        radius
                                        (math/degree->radians start-angle)
                                        (math/degree->radians degree)))
   :draw/line             (fn [{:keys [graphics/shape-drawer]} [sx sy] [ex ey] color]
                            (sd/set-color! shape-drawer color)
                            (sd/line! shape-drawer sx sy ex ey))})

(defn- create-shape-drawer
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))))

(defrecord Graphics []
  cdq.graphics/DrawHandler
  (handle-draws! [graphics draws]
    (doseq [{k 0 :as component} draws
            :when component]
      (apply (draw-fns k) graphics (rest component))))

  disposable/Disposable
  (dispose! [{:keys [graphics/batch
                     graphics/cursors
                     graphics/default-font
                     graphics/shape-drawer-texture
                     graphics/textures]}]
    (disposable/dispose! batch)
    (run! disposable/dispose! (vals cursors))
    (disposable/dispose! default-font)
    (disposable/dispose! shape-drawer-texture)
    (run! disposable/dispose! (vals textures)))

  cdq.graphics/Graphics
  (clear! [{:keys [graphics/core]} [r g b a]]
    (graphics/clear! core r g b a))

  (draw-on-world-viewport!
    [{:keys [graphics/batch
             graphics/shape-drawer
             graphics/unit-scale
             graphics/world-unit-scale
             graphics/world-viewport]}
     f]
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (batch/set-color! batch color/white)
    (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (fn []
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (batch/end! batch))

  (draw-tiled-map!
    [{:keys [graphics/tiled-map-renderer
             graphics/world-viewport]}
     tiled-map
     color-setter]
    (tm-renderer/draw! tiled-map-renderer
                       world-viewport
                       tiled-map
                       color-setter))

  (set-cursor!
    [{:keys [graphics/cursors
             graphics/core]}
     cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! core (get cursors cursor-key)))

  (delta-time
    [{:keys [graphics/core]}]
    (graphics/delta-time core))

  (frames-per-second
    [{:keys [graphics/core]}]
    (graphics/frames-per-second core))

  (world-viewport-width  [{:keys [graphics/world-viewport]}] (:viewport/width  world-viewport))
  (world-viewport-height [{:keys [graphics/world-viewport]}] (:viewport/height world-viewport))

  (camera-position [{:keys [graphics/world-viewport]}] (:camera/position     (:viewport/camera world-viewport)))
  (visible-tiles   [{:keys [graphics/world-viewport]}] (camera/visible-tiles (:viewport/camera world-viewport)))
  (camera-frustum  [{:keys [graphics/world-viewport]}] (camera/frustum       (:viewport/camera world-viewport)))
  (camera-zoom     [{:keys [graphics/world-viewport]}] (:camera/zoom         (:viewport/camera world-viewport)))

  (change-zoom! [{:keys [graphics/world-viewport]} amount]
    (camera/inc-zoom! (:viewport/camera world-viewport) amount))

  (set-camera-position! [{:keys [graphics/world-viewport]} position]
    (camera/set-position! (:viewport/camera world-viewport) position))

  (unproject-ui [{:keys [graphics/ui-viewport]
                  :as graphics}
                 position]
    (assoc graphics :graphics/ui-mouse-position (viewport/unproject ui-viewport position)))

  (unproject-world [{:keys [graphics/world-viewport]
                     :as graphics}
                    position]
    (assoc graphics :graphics/world-mouse-position (viewport/unproject world-viewport position)))

  (update-viewports! [{:keys [graphics/ui-viewport
                              graphics/world-viewport]} width height]
    (viewport/update! ui-viewport    width height {:center? true})
    (viewport/update! world-viewport width height {:center? false}))

  (texture-region [{:keys [graphics/textures]}
                   {:keys [image/file image/bounds]}]
    (assert file)
    (assert (contains? textures file))
    (let [texture (get textures file)]
      (if bounds
        (texture/region texture bounds)
        (texture/region texture)))))

(defn- create-batch [{:keys [graphics/core]
                      :as graphics}]
  (assoc graphics :graphics/batch (graphics/sprite-batch core)))

(defn- create-shape-drawer-texture
  [{:keys [graphics/core]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (graphics/pixmap core 1 1 :pixmap.format/RGBA8888)
                                                                (pixmap/set-color! color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (pixmap/texture pixmap)]
                                                   (disposable/dispose! pixmap)
                                                   texture)))

(defn- assoc-clojure-graphics [graphics clojure-graphics]
  (assoc graphics :graphics/core clojure-graphics))

(defn- create-cursors [{:keys [graphics/core]
                        :as graphics}
                       cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (graphics/pixmap core file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (disposable/dispose! pixmap)
                                                     cursor)))))

(defn- create-default-font [graphics default-font]
  (assoc graphics :graphics/default-font (freetype/generate-font (:file-handle default-font)
                                                                 (:params default-font))))

(defn- create-textures
  [{:keys [graphics/core]
    :as graphics} textures-to-load]
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (graphics/texture core file-handle)]))))

(defn- add-unit-scales [graphics world-unit-scale]
  (assoc graphics
         :graphics/unit-scale (atom 1)
         :graphics/world-unit-scale world-unit-scale))

(defn- tiled-map-renderer [{:keys [graphics/batch
                                   graphics/world-unit-scale]
                            :as graphics}]
  (assoc graphics :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))

(defn- create-ui-viewport
  [{:keys [graphics/core]
    :as graphics} ui-viewport]
  (assoc graphics :graphics/ui-viewport (graphics/fit-viewport core
                                                               (:width  ui-viewport)
                                                               (:height ui-viewport)
                                                               (gdx/orthographic-camera))))

(defn- create-world-viewport
  [{:keys [graphics/core
           graphics/world-unit-scale]
    :as graphics}
   world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (graphics/fit-viewport core
                                                                    world-width
                                                                    world-height
                                                                    (gdx/orthographic-camera
                                                                     :y-down? false
                                                                     :world-width world-width
                                                                     :world-height world-height)))))

(defn- create*
  [{:keys [textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics]
  (-> (map->Graphics {})
      (assoc-clojure-graphics graphics)
      (create-cursors cursors)
      (create-default-font default-font)
      create-batch
      create-shape-drawer-texture
      create-shape-drawer
      (create-textures textures-to-load)
      (add-unit-scales world-unit-scale)
      tiled-map-renderer
      (create-ui-viewport ui-viewport)
      (create-world-viewport world-viewport)))

(defn- graphics-config
  [files {:keys [colors
                 cursors
                 default-font
                 tile-size
                 texture-folder
                 ui-viewport
                 world-viewport]}]
  {:ui-viewport ui-viewport
   :default-font {:file-handle (files/internal files (:path default-font))
                  :params (:params default-font)}
   :colors colors
   :cursors (update-vals (:data cursors)
                         (fn [[short-path hotspot]]
                           [(files/internal files (format (:path-format cursors) short-path))
                            hotspot]))
   :world-unit-scale (float (/ tile-size))
   :world-viewport world-viewport
   :textures-to-load (files-utils/search files texture-folder)})

(def ^:private config
  {:tile-size 48
   :ui-viewport {:width 1440
                 :height 900}
   :world-viewport {:width 1440
                    :height 900}
   :texture-folder {:folder "resources/"
                    :extensions #{"png" "bmp"}}
   :default-font {:path "exocet/films.EXL_____.ttf"
                  :params {:size 16
                           :quality-scaling 2
                           :enable-markup? true
                           :use-integer-positions? false
                           ; :texture-filter/linear because scaling to world-units
                           :min-filter :linear
                           :mag-filter :linear}}
   :colors {"PRETTY_NAME" [0.84 0.8 0.52 1]}
   :cursors {:path-format "cursors/%s.png"
             :data {:cursors/bag                   ["bag001"       [0   0]]
                    :cursors/black-x               ["black_x"      [0   0]]
                    :cursors/default               ["default"      [0   0]]
                    :cursors/denied                ["denied"       [16 16]]
                    :cursors/hand-before-grab      ["hand004"      [4  16]]
                    :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                    :cursors/hand-grab             ["hand003"      [4  16]]
                    :cursors/move-window           ["move002"      [16 16]]
                    :cursors/no-skill-selected     ["denied003"    [0   0]]
                    :cursors/over-button           ["hand002"      [0   0]]
                    :cursors/sandclock             ["sandclock"    [16 16]]
                    :cursors/skill-not-usable      ["x007"         [0   0]]
                    :cursors/use-skill             ["pointer004"   [0   0]]
                    :cursors/walking               ["walking"      [16 16]]}}})

(defn do!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}]
  (colors/put! (:colors config))
  (assoc ctx :ctx/graphics (-> (graphics-config files config)
                               (create* graphics))))
