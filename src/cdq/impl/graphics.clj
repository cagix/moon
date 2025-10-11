(ns cdq.impl.graphics
  (:require [cdq.files :as files-utils]
            [cdq.graphics]
            [cdq.graphics.textures]
            [cdq.graphics.tiled-map-renderer]
            [cdq.graphics.tm-renderer :as tm-renderer]
            [cdq.graphics.ui-viewport]
            [cdq.graphics.world-viewport]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.orthographic-camera :as camera]
            [clojure.gdx.viewport :as viewport]
            [clojure.gdx.shape-drawer :as sd])
  (:import (com.badlogic.gdx.graphics Color
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter
                                      OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils.viewport FitViewport)))

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
    (let [^Texture texture (get textures file)]
      (if bounds
        (let [[x y w h] bounds]
          (TextureRegion. texture (int x) (int y) (int w) (int h)))
        (TextureRegion. texture))))

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
    (viewport/unproject ui-viewport position))

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
    (viewport/unproject world-viewport position))

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
    (.setColor batch Color/WHITE)
    (.setProjectionMatrix batch (camera/combined (viewport/camera world-viewport)))
    (.begin batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1))
    (.end batch)))

(defn create-cursor [files graphics path [hotspot-x hotspot-y]]
  (let [pixmap (Pixmap. (.internal files path))
        cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
    (.dispose pixmap)
    cursor))

(defn generate-font [file-handle {:keys [size
                                         quality-scaling
                                         enable-markup?
                                         use-integer-positions?]}]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator
                            (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
                              (set! (.size params) (* size quality-scaling))
                              (set! (.minFilter params) Texture$TextureFilter/Linear)
                              (set! (.magFilter params) Texture$TextureFilter/Linear)
                              params))]
    (.setScale (.getData font) (/ quality-scaling))
    (set! (.markupEnabled (.getData font)) enable-markup?)
    (.setUseIntegerPositions font use-integer-positions?)
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
  (let [batch (SpriteBatch.)
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor Color/WHITE)
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
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
        (assoc :graphics/shape-drawer (sd/create batch
                                                 (TextureRegion. shape-drawer-texture
                                                                 1
                                                                 0
                                                                 1
                                                                 1)))
        (assoc :graphics/textures (into {} (for [path (files-utils/search files texture-folder)]
                                             [path (Texture. (.internal files path))])))
        (assoc :graphics/unit-scale (atom 1)
               :graphics/world-unit-scale world-unit-scale)
        (assoc :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch))
        (assoc :graphics/ui-viewport (FitViewport. (:width  ui-viewport)
                                                   (:height ui-viewport)
                                                   (OrthographicCamera.)))
        (assoc :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                              world-height (* (:height world-viewport) world-unit-scale)]
                                          (FitViewport. world-width
                                                        world-height
                                                        (doto (OrthographicCamera.)
                                                          (.setToOrtho false world-width world-height))))))))
