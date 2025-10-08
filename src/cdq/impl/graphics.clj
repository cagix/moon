(ns cdq.impl.graphics
  (:require [cdq.graphics]
            [cdq.graphics.tiled-map-renderer]
            [cdq.graphics.ui-viewport]
            [cdq.graphics.world-viewport]
            [clojure.gdx :as gdx]
            [clojure.gdx.bitmap-font :as bitmap-font]
            [clojure.gdx.graphics.orthographic-camera :as camera]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.viewport :as viewport]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.gdx.shape-drawer :as sd])
  (:import (com.badlogic.gdx.graphics Color
                                      Pixmap
                                      Pixmap$Format
                                      Texture)
           (com.badlogic.gdx.graphics.g2d Batch
                                          TextureRegion)))

(def ^:private draw-fns
  {:draw/with-line-width  (fn
                            [{:keys [graphics/shape-drawer]
                              :as graphics}
                             width
                             draws]
                            (sd/with-line-width shape-drawer width
                              (cdq.graphics/draw! graphics draws)) )

   :draw/grid             (fn do!
                            [graphics leftx bottomy gridw gridh cellw cellh color]
                            (let [w (* (float gridw) (float cellw))
                                  h (* (float gridh) (float cellh))
                                  topy (+ (float bottomy) (float h))
                                  rightx (+ (float leftx) (float w))]
                              (doseq [idx (range (inc (float gridw)))
                                      :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
                                (cdq.graphics/draw! graphics
                                                    [[:draw/line [linex topy] [linex bottomy] color]]))
                              (doseq [idx (range (inc (float gridh)))
                                      :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
                                (cdq.graphics/draw! graphics
                                                    [[:draw/line [leftx liney] [rightx liney] color]]))))

   :draw/texture-region   (fn
                            [{:keys [^Batch graphics/batch
                                     graphics/unit-scale
                                     graphics/world-unit-scale]}
                             ^TextureRegion texture-region
                             [x y]
                             & {:keys [center? rotation]}]
                            (let [[w h] (let [dimensions [(.getRegionWidth  texture-region)
                                                          (.getRegionHeight texture-region)]]
                                          (if (= @unit-scale 1)
                                            dimensions
                                            (mapv (comp float (partial * world-unit-scale))
                                                  dimensions)))]
                              (if center?
                                (.draw batch
                                       texture-region
                                       (- (float x) (/ (float w) 2))
                                       (- (float y) (/ (float h) 2))
                                       (/ (float w) 2) ; origin-x
                                       (/ (float h) 2) ; origin-y
                                       w
                                       h
                                       1 ; scale-x
                                       1 ; scale-y
                                       (or rotation 0))
                                (.draw batch
                                       texture-region
                                       (float x)
                                       (float y)
                                       (float w)
                                       (float h)))))

   :draw/text             (fn
                            [{:keys [graphics/batch
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
                            (sd/ellipse! shape-drawer [x y] radius-x radius-y color))

   :draw/filled-ellipse   (fn [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
                            (sd/filled-ellipse! shape-drawer [x y] radius-x radius-y color))

   :draw/circle           (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/circle! shape-drawer [x y] radius color))

   :draw/filled-circle    (fn [{:keys [graphics/shape-drawer]} [x y] radius color]
                            (sd/filled-circle! shape-drawer [x y] radius color))

   :draw/rectangle        (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/rectangle! shape-drawer x y w h color))

   :draw/filled-rectangle (fn [{:keys [graphics/shape-drawer]} x y w h color]
                            (sd/filled-rectangle! shape-drawer x y w h color))

   :draw/arc              (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/arc! shape-drawer [center-x center-y] radius start-angle degree color))

   :draw/sector           (fn [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
                            (sd/sector! shape-drawer [center-x center-y] radius start-angle degree color))

   :draw/line             (fn [{:keys [graphics/shape-drawer]} [sx sy] [ex ey] color]
                            (sd/line! shape-drawer [sx sy] [ex ey] color))})

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
      (apply (draw-fns k) graphics (rest component))))

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
    (sd/with-line-width shape-drawer world-unit-scale
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1))
    (.end batch)))

(defn create!
  [{:keys [colors
           textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics
   gdx]
  (let [batch (gdx/sprite-batch gdx)
        shape-drawer-texture (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                                            (.setColor Color/WHITE)
                                            (.drawPixel 0 0))
                                   texture (Texture. pixmap)]
                               (.dispose pixmap)
                               texture)]
    (-> (map->Graphics {})
        (assoc :graphics/core graphics)
        (assoc :graphics/cursors (update-vals cursors
                                              (fn [[path hotspot]]
                                                (gdx/cursor gdx path hotspot))))
        (assoc :graphics/default-font (gdx/truetype-font gdx
                                                         (:path default-font)
                                                         (:params default-font)))
        (assoc :graphics/batch batch)
        (assoc :graphics/shape-drawer-texture shape-drawer-texture)
        (assoc :graphics/shape-drawer (gdx/shape-drawer gdx
                                                        batch
                                                        (TextureRegion. shape-drawer-texture
                                                                        1
                                                                        0
                                                                        1
                                                                        1)))
        (assoc :graphics/textures (into {} (for [[path _file-handle] textures-to-load]
                                             [path (gdx/texture gdx path)])))
        (assoc :graphics/unit-scale (atom 1)
               :graphics/world-unit-scale world-unit-scale)
        (assoc :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch))
        (assoc :graphics/ui-viewport (gdx/viewport gdx
                                                   (:width  ui-viewport)
                                                   (:height ui-viewport)
                                                   (camera/create)))
        (assoc :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                              world-height (* (:height world-viewport) world-unit-scale)]
                                          (gdx/viewport gdx
                                                        world-width
                                                        world-height
                                                        (camera/create
                                                         :y-down? false
                                                         :world-width world-width
                                                         :world-height world-height)))))))
