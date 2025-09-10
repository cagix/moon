(ns cdq.gdx.graphics
  (:require [cdq.utils :as utils] ; <- name with clojure !

            ;; pass file-handles !
            [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            ;;

            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.gdx.utils.viewport :as viewport]))

(def font-file "exocet/films.EXL_____.ttf")
(def font-params {:size 16
             :quality-scaling 2
             :enable-markup? true
             ; false, otherwise scaling to world-units not visible
             :use-integer-positions? false})

(def world-viewport-width 1440)
(def world-viewport-height 900)

(def path-format "cursors/%s.png")

(def cursor-data
  {:cursors/bag                   ["bag001"       [0   0]]
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
   :cursors/walking               ["walking"      [16 16]]})

(defn create
  [graphics
   {:keys [
           ; cursor-data
           ; path-format
           textures-to-load
           world-unit-scale
           ; world-viewport-width
           ; world-viewport-height
           ; font-file
           ; font-params
           ]}]
  (let [batch (sprite-batch/create)
        shape-drawer-texture (let [pixmap (doto (pixmap/create)
                                            (pixmap/set-color! color/white)
                                            (pixmap/draw-pixel! 0 0))
                                   texture (texture/create pixmap)]
                               (pixmap/dispose! pixmap)
                               texture)]
    {:ctx/batch batch
     :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale batch)
     :ctx/graphics graphics
     :ctx/textures (into {} (for [[path file-handle] textures-to-load]
                              [path (texture/from-file file-handle)]))
     :ctx/cursors (update-vals cursor-data
                               (fn [[file [hotspot-x hotspot-y]]]
                                 (let [pixmap (pixmap/create (files/internal (gdx/files) (format path-format file)))
                                       cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                                   (.dispose pixmap)
                                   cursor)))
     :ctx/world-viewport (let [world-width  (* world-viewport-width world-unit-scale)
                               world-height (* world-viewport-height world-unit-scale)]
                           (viewport/fit world-width
                                         world-height
                                         (camera/orthographic :y-down? false
                                                              :world-width world-width
                                                              :world-height world-height)))
     :ctx/default-font (freetype/generate-font (files/internal (gdx/files) font-file)
                                               font-params)
     :ctx/shape-drawer-texture shape-drawer-texture
     :ctx/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))}))

(defn dispose!
  [{:keys [ctx/batch
           ctx/cursors
           ctx/default-font
           ctx/shape-drawer-texture
           ctx/textures]}]
  (disposable/dispose! batch)
  (run! disposable/dispose! (vals cursors))
  (disposable/dispose! default-font)
  (disposable/dispose! shape-drawer-texture)
  (run! disposable/dispose! (vals textures)))

(defn draw-texture-region!
  [{:keys [ctx/batch
           ctx/unit-scale
           ctx/world-unit-scale]}
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

(defn draw-arc!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))

(defn draw-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/circle! shape-drawer x y radius))

(defn draw-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-circle! shape-drawer x y radius))

(defn draw-filled-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/filled-rectangle! shape-drawer x y w h))

(defn draw-line!
  [{:keys [ctx/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/line! shape-drawer sx sy ex ey))

(defn draw-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/rectangle! shape-drawer x y w h))

(defn draw-sector!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer (color/->obj color))
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (utils/degree->radians start-angle)
              (utils/degree->radians degree)))

(defn draw-text!
  [{:keys [ctx/batch
           ctx/unit-scale
           ctx/default-font]}
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

(defn with-line-width
  [{:keys [ctx/shape-drawer]} width f]
  (sd/with-line-width shape-drawer width f))

(defn draw-on-world-viewport!
  [{:keys [ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]}
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

(defn draw-tiled-map!
  [{:keys [ctx/tiled-map-renderer
           ctx/world-viewport]}
   tiled-map
   color-setter]
  (tm-renderer/draw! tiled-map-renderer
                     world-viewport
                     tiled-map
                     color-setter))

(defn set-cursor!
  [{:keys [ctx/cursors
           ctx/graphics]}
   cursor-key]
  (assert (contains? cursors cursor-key))
  (graphics/set-cursor! graphics (get cursors cursor-key)))

(defn delta-time
  [{:keys [ctx/graphics]}]
  (graphics/delta-time graphics))

(defn frames-per-second
  [{:keys [ctx/graphics]}]
  (graphics/frames-per-second graphics))

(def camera-position (comp :camera/position :viewport/camera :ctx/world-viewport))
