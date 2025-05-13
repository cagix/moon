(ns cdq.g.graphics
  (:require cdq.graphics
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.math :refer [degree->radians]]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.graphics.g2d Batch SpriteBatch TextureRegion)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport Viewport)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [^TextureRegion texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions [(.getRegionWidth  texture-region)
                                              (.getRegionHeight texture-region)]
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defn- set-color! [^ShapeDrawer shape-drawer color]
  (.setColor shape-drawer (interop/->color color)))

(defrecord Graphics [^Batch batch
                     ^Texture shape-drawer-texture
                     ^ShapeDrawer shape-drawer
                     cursors
                     default-font
                     world-unit-scale
                     world-viewport
                     get-tiled-map-renderer
                     unit-scale
                     ui-viewport]
  com.badlogic.gdx.utils.Disposable
  (dispose [_]
    (.dispose batch)
    (.dispose shape-drawer-texture)
    (run! Disposable/.dispose (vals cursors))
    (Disposable/.dispose default-font))

  cdq.graphics/Graphics
  (mouse-position [_]
    ; TODO mapv int needed?
    (mapv int (graphics/unproject-mouse-position ui-viewport)))

  (world-mouse-position [_]
    ; TODO clamping only works for gui-viewport ? check. comment if true
    ; TODO ? "Can be negative coordinates, undefined cells."
    (graphics/unproject-mouse-position world-viewport))

  (pixels->world-units [_ pixels]
    (* (int pixels) world-unit-scale))

  (draw-image [_ {:keys [texture-region color] :as image} position]
    (graphics/draw-texture-region batch
                                  texture-region
                                  position
                                  (unit-dimensions image @unit-scale)
                                  0 ; rotation
                                  color))

  (draw-rotated-centered [_ {:keys [texture-region color] :as image} rotation [x y]]
    (let [[w h] (unit-dimensions image @unit-scale)]
      (graphics/draw-texture-region batch
                                    texture-region
                                    [(- (float x) (/ (float w) 2))
                                     (- (float y) (/ (float h) 2))]
                                    [w h]
                                    rotation
                                    color)))

  (set-camera-position! [_ position]
    (camera/set-position! (:camera world-viewport) position))

  (draw-text [_ {:keys [font scale x y text h-align up?]}]
    (graphics/draw-text! {:font (or font default-font)
                          :scale (* (float @unit-scale)
                                    (float (or scale 1)))
                          :batch batch
                          :x x
                          :y y
                          :text text
                          :h-align h-align
                          :up? up?}))

  (draw-ellipse [_ [x y] radius-x radius-y color]
    (set-color! shape-drawer color)
    (.ellipse shape-drawer
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (draw-filled-ellipse [_ [x y] radius-x radius-y color]
    (set-color! shape-drawer color)
    (.filledEllipse shape-drawer
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (draw-circle [_ [x y] radius color]
    (set-color! shape-drawer color)
    (.circle shape-drawer
             (float x)
             (float y)
             (float radius)))

  (draw-filled-circle [_ [x y] radius color]
    (set-color! shape-drawer color)
    (.filledCircle shape-drawer
                   (float x)
                   (float y)
                   (float radius)))

  (draw-arc [_ [center-x center-y] radius start-angle degree color]
    (set-color! shape-drawer color)
    (.arc shape-drawer
          (float center-x)
          (float center-y)
          (float radius)
          (float (degree->radians start-angle))
          (float (degree->radians degree))))

  (draw-sector [_ [center-x center-y] radius start-angle degree color]
    (set-color! shape-drawer color)
    (.sector shape-drawer
             (float center-x)
             (float center-y)
             (float radius)
             (float (degree->radians start-angle))
             (float (degree->radians degree))))

  (draw-rectangle [_ x y w h color]
    (set-color! shape-drawer color)
    (.rectangle shape-drawer
                (float x)
                (float y)
                (float w)
                (float h)))

  (draw-filled-rectangle [_ x y w h color]
    (set-color! shape-drawer color)
    (.filledRectangle shape-drawer
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (draw-line [_ [sx sy] [ex ey] color]
    (set-color! shape-drawer color)
    (.line shape-drawer
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [_ width draw-fn]
    (let [old-line-width (.getDefaultLineWidth shape-drawer)]
      (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth shape-drawer (float old-line-width))))

  (draw-grid [this leftx bottomy gridw gridh cellw cellh color]
    (let [w (* (float gridw) (float cellw))
          h (* (float gridh) (float cellh))
          topy (+ (float bottomy) (float h))
          rightx (+ (float leftx) (float w))]
      (doseq [idx (range (inc (float gridw)))
              :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
        (cdq.graphics/draw-line this [linex topy] [linex bottomy] color))
      (doseq [idx (range (inc (float gridh)))
              :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
        (cdq.graphics/draw-line this [leftx liney] [rightx liney] color))))

  (draw-on-world-view! [this f]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
    (.begin batch)
    (cdq.graphics/with-line-width this world-unit-scale
      (fn []
        ; could pass new 'g' with assoc :unit-scale -> but using ctx/graphics accidentally
        ; -> icon is drawn at too big ! => mutable field.
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (set-cursor! [_ cursor-key]
    (.setCursor Gdx/graphics (utils/safe-get cursors cursor-key)))

  (draw-tiled-map [_ tiled-map color-setter]
    (tiled/draw! (get-tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (resize! [_ width height]
    (Viewport/.update ui-viewport    width height true)
    (Viewport/.update world-viewport width height false))

  (sub-sprite [_ sprite [x y w h]]
    (sprite* (TextureRegion. ^TextureRegion (:texture-region sprite)
                             (int x)
                             (int y)
                             (int w)
                             (int h))
             world-unit-scale))

  (sprite-sheet [_ texture tilew tileh]
    {:image (sprite* (TextureRegion. ^Texture texture) world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (from-sheet [this {:keys [image tilew tileh]} [x y]]
    (cdq.graphics/sub-sprite this
                             image
                             [(* x tilew)
                              (* y tileh)
                              tilew
                              tileh]))

  (sprite [_ texture]
    (sprite* (TextureRegion. ^Texture texture) world-unit-scale)))

(defn create [{:keys [cursors
                      default-font
                      tile-size
                      world-viewport
                      ui-viewport]}]
  (let [batch (SpriteBatch.)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ tile-size))]
    (map->Graphics
     {:batch batch
      :shape-drawer-texture shape-drawer-texture
      :shape-drawer (ShapeDrawer. batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
      :cursors (utils/mapvals
                (fn [[file [hotspot-x hotspot-y]]]
                  (let [pixmap (graphics/pixmap (str "cursors/" file ".png"))
                        cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                    (.dispose pixmap)
                    cursor))
                cursors)
      :default-font (graphics/truetype-font default-font)
      :world-unit-scale  world-unit-scale
      :world-viewport (graphics/world-viewport world-unit-scale world-viewport)
      :get-tiled-map-renderer (memoize (fn [tiled-map]
                                         (tiled/renderer tiled-map
                                                         world-unit-scale
                                                         batch)))
      :ui-viewport (graphics/fit-viewport (:width  ui-viewport)
                                          (:height ui-viewport))
      :unit-scale (atom 1)})))
