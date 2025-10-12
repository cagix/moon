(ns cdq.impl.graphics
  (:require [cdq.files :as files-utils]
            [cdq.graphics]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.textures]
            [cdq.graphics.tiled-map-renderer]
            [cdq.graphics.tm-renderer :as tm-renderer]
            [cdq.graphics.ui-viewport]
            [cdq.graphics.world-viewport]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.pixmap.format :as pixmap.format]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.texture.filter :as texture.filter]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.graphics.g2d.shape-drawer :as sd]
            [clojure.gdx.graphics.g2d.bitmap-font :as fnt]
            [clojure.gdx.graphics.g2d.bitmap-font.data :as data]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.graphics.g2d.freetype.generator :as generator]
            [clojure.gdx.graphics.g2d.freetype.generator.parameter :as parameter]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]
            [clojure.gdx.math.vector2 :as vector2]
            [clojure.gdx.utils.viewport :as viewport]))

(defn- unproject [viewport [x y]]
  (-> viewport
      (viewport/unproject (vector2/->java x y))
      vector2/->clj))

(def ^:private draw-fns
  (update-vals '{:draw/with-line-width  cdq.graphics.draw.with-line-width/do!
                 :draw/grid             cdq.graphics.draw.grid/do!
                 :draw/texture-region   cdq.graphics.draw.texture-region/do!
                 :draw/text             cdq.graphics.draw.text/do!
                 :draw/ellipse          cdq.graphics.draw.ellipse/do!
                 :draw/filled-ellipse   cdq.graphics.draw.filled-ellipse/do!
                 :draw/circle           cdq.graphics.draw.circle/do!
                 :draw/filled-circle    cdq.graphics.draw.filled-circle/do!
                 :draw/rectangle        cdq.graphics.draw.rectangle/do!
                 :draw/filled-rectangle cdq.graphics.draw.filled-rectangle/do!
                 :draw/arc              cdq.graphics.draw.arc/do!
                 :draw/sector           cdq.graphics.draw.sector/do!
                 :draw/line             cdq.graphics.draw.line/do!}
               requiring-resolve))

(defrecord Graphics []
  cdq.graphics.textures/Textures
  (texture-region [{:keys [graphics/textures]}
                   {:keys [image/file image/bounds]}]
    (assert file)
    (assert (contains? textures file))
    (let [texture (get textures file)]
      (if bounds
        (texture-region/create texture bounds)
        (texture-region/create texture))))

  cdq.graphics.tiled-map-renderer/TiledMapRenderer
  (draw!
    [{:keys [graphics/tiled-map-renderer
             graphics/world-viewport]}
     tiled-map
     color-setter]
    (tm-renderer/draw! tiled-map-renderer
                       world-viewport
                       tiled-map
                       color-setter))

  cdq.graphics.ui-viewport/UIViewport
  (unproject [{:keys [graphics/ui-viewport]} position]
    (unproject ui-viewport position))

  (update! [{:keys [graphics/ui-viewport]} width height]
    (viewport/update! ui-viewport width height {:center? true}))

  cdq.graphics/Graphics
  (draw! [graphics draws]
    (doseq [{k 0 :as component} draws
            :when component]
      (apply (draw-fns k) graphics (rest component)))))

(extend-type Graphics
  cdq.graphics.world-viewport/WorldViewport
  (width [{:keys [graphics/world-viewport]}]
    (viewport/world-width world-viewport))

  (height [{:keys [graphics/world-viewport]}]
    (viewport/world-height world-viewport))

  (unproject [{:keys [graphics/world-viewport]} position]
    (unproject world-viewport position))

  (update! [{:keys [graphics/world-viewport]} width height]
    (viewport/update! world-viewport width height {:center? false}))

  (draw! [{:keys [graphics/batch
                  graphics/shape-drawer
                  graphics/unit-scale
                  graphics/world-unit-scale
                  graphics/world-viewport]}
          f]
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (.setColor batch color/white)
    (.setProjectionMatrix batch (camera/combined (viewport/camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1))
    (.end batch)))

(defn create-cursor [files graphics path [hotspot-x hotspot-y]]
  (let [pixmap (pixmap/create (.internal files path))
        cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
    (pixmap/dispose! pixmap)
    cursor))

(defn generate-font [file-handle {:keys [size
                                         quality-scaling
                                         enable-markup?
                                         use-integer-positions?]}]
  (let [generator (generator/create file-handle)
        font (generator/font generator
                             (parameter/create
                              {:size (* size quality-scaling)
                               :min-filter texture.filter/linear
                               :mag-filter texture.filter/linear}))]
    (generator/dispose! generator)
    (data/set-scale! (fnt/data font) (/ quality-scaling))
    (data/set-enable-markup! (fnt/data font) enable-markup?)
    (fnt/use-integer-positions! font use-integer-positions?)
    font))

(defn create!
  [graphics
   files
   {:keys [colors
           cursors
           default-font
           texture-folder
           tile-size
           ui-viewport
           world-viewport]}]
  (doseq [[name rgba] colors]
    (colors/put! name (color/create rgba)))
  (let [batch (sprite-batch/create)
        shape-drawer-texture (let [pixmap (doto (pixmap/create 1 1 pixmap.format/rgba8888)
                                            (pixmap/set-color! color/white)
                                            (pixmap/draw-pixel! 0 0))
                                   texture (texture/create pixmap)]
                               (pixmap/dispose! pixmap)
                               texture)
        world-unit-scale (float (/ tile-size))]
    (-> (map->Graphics {})
        (assoc :graphics/core graphics)
        (assoc :graphics/cursors (update-vals (:data cursors)
                                              (fn [[path hotspot]]
                                                (create-cursor files
                                                               graphics
                                                               (format (:path-format cursors) path)
                                                               hotspot))))
        (assoc :graphics/default-font (generate-font (.internal files (:path default-font))
                                                     (:params default-font)))
        (assoc :graphics/batch batch)
        (assoc :graphics/shape-drawer-texture shape-drawer-texture)
        (assoc :graphics/shape-drawer (sd/create batch (texture-region/create shape-drawer-texture 1 0 1 1)))
        (assoc :graphics/textures (into {} (for [path (files-utils/search files texture-folder)]
                                             [path (texture/create path)])))
        (assoc :graphics/unit-scale (atom 1)
               :graphics/world-unit-scale world-unit-scale)
        (assoc :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch))
        (assoc :graphics/ui-viewport (fit-viewport/create (:width  ui-viewport)
                                                          (:height ui-viewport)
                                                          (orthographic-camera/create)))
        (assoc :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                              world-height (* (:height world-viewport) world-unit-scale)]
                                          (fit-viewport/create world-width
                                                               world-height
                                                               (doto (orthographic-camera/create)
                                                                 (orthographic-camera/set-to-ortho! false world-width world-height))))))))
