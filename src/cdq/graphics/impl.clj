(ns cdq.graphics.impl
  (:require [cdq.graphics]
            [cdq.graphics.shape-drawer :as shape-drawer]
            [cdq.graphics.shape-drawer-texture :as shape-drawer-texture]
            [cdq.graphics.sprite-batch :as sprite-batch]
            [cdq.graphics.textures :as textures]
            [cdq.graphics.tiled-map :as tiled-map]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.unit-scale :as unit-scale]
            [cdq.graphics.world-viewport :as world-viewport]
            [clojure.graphics.color]
            [clojure.graphics.freetype :as freetype]
            [clojure.graphics.orthographic-camera :as camera]
            [clojure.graphics.viewport :as viewport]
            [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.g2d.batch :as batch]
            [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.graphics.g2d.texture-region :as texture-region]
            [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.colors :as colors]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [gdl.disposable :as disposable]
            [gdl.files :as files]
            [gdl.files.utils :as files-utils]
            [gdl.math :refer [degree->radians]]
            [space.earlygrey.shape-drawer :as sd]))

(defn- create-font [graphics default-font]
  (assoc graphics :graphics/default-font (freetype/generate-font (:file-handle default-font)
                                                                 (:params default-font))))

(defn- create-cursors
  [{:keys [graphics/core]
    :as graphics}
   cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (graphics/pixmap core file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (disposable/dispose! pixmap)
                                                     cursor)))))

(def ^:private draw-fns
  {:draw/with-line-width  (fn [{:keys [graphics/shape-drawer]
                                :as graphics}
                               width
                               draws]
                            (sd/with-line-width shape-drawer width
                              (cdq.graphics/handle-draws! graphics draws)))
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
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/ellipse! shape-drawer x y radius-x radius-y))
   :draw/filled-ellipse   (fn [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/filled-ellipse! shape-drawer x y radius-x radius-y))
   :draw/circle           (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/circle! shape-drawer x y radius))
   :draw/filled-circle    (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/filled-circle! shape-drawer x y radius))
   :draw/rectangle        (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/rectangle! shape-drawer x y w h))
   :draw/filled-rectangle (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/filled-rectangle! shape-drawer x y w h))
   :draw/arc              (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/arc! shape-drawer
                                     center-x
                                     center-y
                                     radius
                                     (degree->radians start-angle)
                                     (degree->radians degree)))
   :draw/sector           (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/sector! shape-drawer
                                        center-x
                                        center-y
                                        radius
                                        (degree->radians start-angle)
                                        (degree->radians degree)))
   :draw/line             (fn [{:keys [graphics/shape-drawer]} [sx sy] [ex ey] color]
                            (sd/set-color! shape-drawer (color/float-bits color))
                            (sd/line! shape-drawer sx sy ex ey))})

(defrecord RGraphics []
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

  cdq.graphics/Draws
  (handle-draws! [graphics draws]
    (doseq [{k 0 :as component} draws
            :when component]
      (apply (draw-fns k) graphics (rest component))))

  cdq.graphics/DrawOnWorldViewport
  (draw-on-world-viewport! [{:keys [graphics/batch
                                    graphics/shape-drawer
                                    graphics/unit-scale
                                    graphics/world-unit-scale
                                    graphics/world-viewport]}
                            f]
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (batch/set-color! batch clojure.graphics.color/white)
    (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1))
    (batch/end! batch))

  cdq.graphics/PGraphics
  (clear! [{:keys [graphics/core]} [r g b a]]
    (graphics/clear! core r g b a))

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
    (viewport/update! world-viewport width height {:center? false})))

(defn- create*
  [{:keys [textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics]
  (-> (map->RGraphics {})
      (assoc :graphics/core graphics)
      (create-cursors cursors)
      (create-font default-font)
      sprite-batch/create
      shape-drawer-texture/create
      shape-drawer/create
      (textures/create textures-to-load)
      (unit-scale/create world-unit-scale)
      tiled-map/renderer
      (ui-viewport/create ui-viewport)
      (world-viewport/create world-viewport)))

(defn- handle-files
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

(defn create [graphics files params]
  (doseq [[name rgba] (:colors params)]
    (colors/put! name (color/create rgba)))
  (-> (handle-files files params)
      (create* graphics)))
