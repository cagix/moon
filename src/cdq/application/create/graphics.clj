(ns cdq.application.create.graphics
  (:require [cdq.files]
            [cdq.graphics]
            [com.badlogic.gdx.graphics.colors :as colors]
            [com.badlogic.gdx.graphics.orthographic-camera :as camera]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [com.badlogic.gdx.graphics.texture :as texture]
            [com.badlogic.gdx.graphics.g2d.freetype :as freetype]
            [com.badlogic.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [com.badlogic.gdx.utils.viewport :as viewport]
            [gdl.files :as files]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]
            [gdl.graphics.g2d.batch :as batch]
            [space.earlygrey.shape-drawer :as sd]))

(defrecord RGraphics []
  cdq.graphics/Graphics
  (clear! [{:keys [ctx/graphics]} [r g b a]]
    (graphics/clear! graphics r g b a))

  (dispose!
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

  (draw-on-world-viewport!
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

  (draw-tiled-map!
    [{:keys [ctx/tiled-map-renderer
             ctx/world-viewport]}
     tiled-map
     color-setter]
    (tm-renderer/draw! tiled-map-renderer
                       world-viewport
                       tiled-map
                       color-setter))

  (set-cursor!
    [{:keys [ctx/cursors
             ctx/graphics]}
     cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key)))

  (delta-time
    [{:keys [ctx/graphics]}]
    (graphics/delta-time graphics))

  (frames-per-second
    [{:keys [ctx/graphics]}]
    (graphics/frames-per-second graphics))

  (world-viewport-width  [{:keys [ctx/world-viewport]}] (:viewport/width  world-viewport))
  (world-viewport-height [{:keys [ctx/world-viewport]}] (:viewport/height world-viewport))

  (camera-position [{:keys [ctx/world-viewport]}] (:camera/position     (:viewport/camera world-viewport)))
  (visible-tiles   [{:keys [ctx/world-viewport]}] (camera/visible-tiles (:viewport/camera world-viewport)))
  (camera-frustum  [{:keys [ctx/world-viewport]}] (camera/frustum       (:viewport/camera world-viewport)))
  (camera-zoom     [{:keys [ctx/world-viewport]}] (:camera/zoom         (:viewport/camera world-viewport)))

  (change-zoom! [{:keys [ctx/world-viewport]} amount]
    (camera/inc-zoom! (:viewport/camera world-viewport) amount))

  (set-camera-position! [{:keys [ctx/world-viewport]} position]
    (camera/set-position! (:viewport/camera world-viewport) position))

  (unproject-ui    [{:keys [ctx/ui-viewport]}    position] (viewport/unproject-clamp ui-viewport    position))
  (unproject-world [{:keys [ctx/world-viewport]} position] (viewport/unproject-clamp world-viewport position))

  (update-viewports! [{:keys [ctx/ui-viewport
                              ctx/world-viewport]} width height]
    (viewport/update! ui-viewport    width height :center? true)
    (viewport/update! world-viewport width height :center? false))

  (texture-region [{:keys [ctx/textures]}
                   {:keys [image/file image/bounds]}]
    (assert file)
    (assert (contains? textures file))
    (let [texture (get textures file)]
      (if bounds
        (texture/region texture bounds)
        (texture/region texture)))))

(defn- create*
  [graphics
   {:keys [colors
           textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}]
  (colors/put! colors)
  (let [batch (sprite-batch/create)
        shape-drawer-texture (let [pixmap (doto (pixmap/create)
                                            (pixmap/set-color! color/white)
                                            (pixmap/draw-pixel! 0 0))
                                   texture (texture/create pixmap)]
                               (pixmap/dispose! pixmap)
                               texture)]
    (merge (map->RGraphics {})
           {:ctx/batch batch
            :ctx/cursors (update-vals cursors
                                      (fn [[file-handle [hotspot-x hotspot-y]]]
                                        (let [pixmap (pixmap/create file-handle)
                                              cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                                          (.dispose pixmap)
                                          cursor)))
            :ctx/default-font (freetype/generate-font (:file-handle default-font) (:params default-font))
            :ctx/graphics graphics
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))
            :ctx/textures (into {} (for [[path file-handle] textures-to-load]
                                     [path (texture/from-file file-handle)]))
            :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale batch)
            :ctx/ui-viewport (viewport/fit (:width  ui-viewport)
                                           (:height ui-viewport)
                                           (camera/orthographic))
            :ctx/unit-scale (atom 1)
            :ctx/world-unit-scale world-unit-scale
            :ctx/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                      world-height (* (:height world-viewport) world-unit-scale)]
                                  (viewport/fit world-width
                                                world-height
                                                (camera/orthographic :y-down? false
                                                                     :world-width world-width
                                                                     :world-height world-height)))})))

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
   :textures-to-load (cdq.files/search files texture-folder)})

(defn- extend-draws [graphics draw-fns]
  (extend-type (class graphics)
    cdq.graphics/DrawHandler
    (handle-draws! [graphics draws]
      (doseq [{k 0 :as component} draws
              :when component]
        (apply (draw-fns k) graphics (rest component)))))
  graphics)

(defn do!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}
   config]
  (assoc ctx :ctx/graphics (let [graphics (create* graphics (graphics-config files config))]
                             (-> graphics
                                 (extend-draws (:draw-fns config))))))
