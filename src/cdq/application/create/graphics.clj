(ns cdq.application.create.graphics
  (:require [cdq.application.create.graphics.shape-drawer]
            [cdq.files]
            [cdq.graphics]
            [com.badlogic.gdx.graphics.colors :as colors]
            [com.badlogic.gdx.graphics.orthographic-camera :as camera]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [com.badlogic.gdx.graphics.texture :as texture]
            [com.badlogic.gdx.graphics.g2d.freetype :as freetype]
            [com.badlogic.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [com.badlogic.gdx.utils.viewport.fit-viewport :as viewport]
            [gdl.files :as files]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]))

(defrecord Graphics [])

(defn- create-batch [graphics]
  (assoc graphics :graphics/batch (sprite-batch/create)))

(defn- create-shape-drawer-texture [graphics]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (pixmap/create)
                                                                (pixmap/set-color! color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (texture/create pixmap)]
                                                   (pixmap/dispose! pixmap)
                                                   texture)))

(defn- assoc-gdx [graphics gdx-graphics]
  (assoc graphics :graphics/core gdx-graphics))

(defn- create-cursors [{:keys [graphics/core]
                        :as graphics}
                       cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (pixmap/create file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (.dispose pixmap)
                                                     cursor)))))

(defn- create-default-font [graphics default-font]
  (assoc graphics :graphics/default-font (freetype/generate-font (:file-handle default-font)
                                                                 (:params default-font))))

(defn- create-textures [graphics textures-to-load]
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (texture/from-file file-handle)]))))

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
      (assoc-gdx graphics)
      (create-cursors cursors)
      (create-default-font default-font)
      create-batch
      create-shape-drawer-texture
      cdq.application.create.graphics.shape-drawer/do!
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
                               ((requiring-resolve 'cdq.application.create.graphics.protocols/do!)))))
