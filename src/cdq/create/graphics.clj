(ns cdq.create.graphics
  (:require [cdq.create.graphics.shape-drawer]
            [cdq.files]
            [cdq.graphics]
            [gdl.disposable :as disposable]
            [gdl.files :as files]
            [gdl.graphics :as graphics]
            [gdl.graphics.pixmap :as pixmap]
            [gdl.graphics.color :as color]
            [gdl.impl.camera :as camera]
            [gdl.impl.colors :as colors]
            [gdl.impl.freetype :as freetype]
            [gdl.impl.sprite-batch :as sprite-batch]
            [gdl.impl.tiled-map-renderer :as tm-renderer]
            [gdl.impl.viewport :as viewport]))

(defrecord Graphics [])

(defn- create-batch [graphics]
  (assoc graphics :graphics/batch (sprite-batch/create)))

(defn- create-shape-drawer-texture
  [{:keys [graphics/core]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (graphics/pixmap core 1 1 :pixmap.format/RGBA8888)
                                                                (pixmap/set-color! color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (pixmap/texture pixmap)]
                                                   (disposable/dispose! pixmap)
                                                   texture)))

(defn- assoc-gdl-graphics [graphics gdl-graphics]
  (assoc graphics :graphics/core gdl-graphics))

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

(defn- create-ui-viewport [graphics ui-viewport]
  (assoc graphics :graphics/ui-viewport (viewport/create (:width  ui-viewport)
                                                         (:height ui-viewport)
                                                         (camera/orthographic))))

(defn- create-world-viewport [{:keys [graphics/world-unit-scale]
                               :as graphics}
                              world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (viewport/create world-width
                                                              world-height
                                                              (camera/orthographic :y-down? false
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
      (assoc-gdl-graphics graphics)
      (create-cursors cursors)
      (create-default-font default-font)
      create-batch
      create-shape-drawer-texture
      cdq.create.graphics.shape-drawer/do!
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
  (colors/put! (:colors config))
  (assoc ctx :ctx/graphics (-> (graphics-config files config)
                               (create* graphics)
                               (extend-draws (:draw-fns config))
                               ((requiring-resolve 'cdq.create.graphics.protocols/do!)))))
