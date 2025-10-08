(ns cdq.impl.graphics
  (:require [cdq.graphics]
            [cdq.graphics.tiled-map-renderer]
            [cdq.graphics.ui-viewport]
            [cdq.graphics.world-viewport]
            [clojure.gdx.graphics.orthographic-camera :as camera]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.viewport :as viewport]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer])
  (:import (com.badlogic.gdx.graphics Color
                                      Colors
                                      Pixmap
                                      Pixmap$Format
                                      Texture
                                      Texture$TextureFilter)
           (com.badlogic.gdx.graphics.g2d Batch
                                          SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.utils.viewport FitViewport)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- generate-font [file-handle params]
  (let [{:keys [size
                quality-scaling
                enable-markup?
                use-integer-positions?
                min-filter
                mag-filter]} params]
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
      font)))

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
  (clear! [{:keys [graphics/core]} color]
    (graphics/clear! core color))

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
    (graphics/frames-per-second core)))

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
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (* world-unit-scale old-line-width))
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1)
      (.setDefaultLineWidth shape-drawer old-line-width))
    (.end batch)))

(defn create!
  [{:keys [colors
           textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics]
  (doseq [[name [r g b a]] colors]
    (Colors/put name (Color. r g b a)))
  (let [batch (SpriteBatch.)
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor Color/WHITE)
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
                               texture)]
    (-> (map->Graphics {})
        (assoc :graphics/core graphics)
        (assoc :graphics/cursors (update-vals cursors
                                              (fn [[file-handle [hotspot-x hotspot-y]]]
                                                (let [pixmap (Pixmap. file-handle)
                                                      cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                                                  (.dispose pixmap)
                                                  cursor))))
        (assoc :graphics/default-font (generate-font (:file-handle default-font)
                                                     (:params default-font)))
        (assoc :graphics/batch batch)
        (assoc :graphics/shape-drawer-texture shape-drawer-texture)
        (assoc :graphics/shape-drawer (ShapeDrawer. batch
                                                    (TextureRegion. shape-drawer-texture
                                                                    1
                                                                    0
                                                                    1
                                                                    1)))
        (assoc :graphics/textures (into {} (for [[path file-handle] textures-to-load]
                                             [path (Texture. file-handle)])))
        (assoc :graphics/unit-scale (atom 1)
               :graphics/world-unit-scale world-unit-scale)
        (assoc :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch))
        (assoc :graphics/ui-viewport (FitViewport. (:width  ui-viewport)
                                                   (:height ui-viewport)
                                                   (camera/create)))
        (assoc :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                              world-height (* (:height world-viewport) world-unit-scale)]
                                          (FitViewport. world-width
                                                        world-height
                                                        (camera/create
                                                         :y-down? false
                                                         :world-width world-width
                                                         :world-height world-height)))))))
