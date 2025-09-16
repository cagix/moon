(ns cdq.create.graphics
  (:require [cdq.ctx.graphics]
            [clojure.gdx.scene2d.ctx]
            [cdq.files]
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.graphics.color :as color]))

(defrecord RGraphics [])

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

(defn graphics-config
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

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   config]
  (extend-type (class ctx)
    clojure.gdx.scene2d.ctx/Graphics
    (draw! [{:keys [ctx/graphics]} draws]
      (cdq.ctx.graphics/handle-draws! graphics draws)))
  (assoc ctx :ctx/graphics (let [{:keys [clojure.gdx/files
                                         clojure.gdx/graphics]} gdx
                                 draw-fns (:draw-fns config)
                                 graphics (create* graphics (graphics-config files config))]
                             (extend-type (class graphics)
                               cdq.ctx.graphics/DrawHandler
                               (handle-draws! [graphics draws]
                                 (doseq [{k 0 :as component} draws
                                         :when component]
                                   (apply (draw-fns k) graphics (rest component)))))
                             (assoc graphics
                                    :graphics/entity-render-layers (:graphics/entity-render-layers config)))))
