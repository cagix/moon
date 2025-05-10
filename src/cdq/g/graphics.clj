(ns cdq.g.graphics
  (:require cdq.graphics
            [clojure.gdx :as gdx]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.shape-drawer :as shape-drawer]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.disposable :refer [dispose!]]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.graphics.g2d Batch TextureRegion)
           (com.badlogic.gdx.utils.viewport Viewport)))

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

(defrecord Graphics [^Batch batch
                     ^Texture shape-drawer-texture
                     shape-drawer
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
    (run! dispose! (vals cursors))
    (dispose! default-font))

  cdq.graphics/Graphics
  (clear-screen! [_]
    (graphics/clear-screen!))

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
    (shape-drawer/ellipse! shape-drawer x y radius-x radius-y color))

  (draw-filled-ellipse [_ [x y] radius-x radius-y color]
    (shape-drawer/filled-ellipse! shape-drawer x y radius-x radius-y color))

  (draw-circle [_ [x y] radius color]
    (shape-drawer/circle! shape-drawer x y radius color))

  (draw-filled-circle [_ [x y] radius color]
    (shape-drawer/filled-circle! shape-drawer x y radius color))

  (draw-arc [_ [center-x center-y] radius start-angle degree color]
    (shape-drawer/arc! shape-drawer center-x center-y radius start-angle degree color))

  (draw-sector [_ [center-x center-y] radius start-angle degree color]
    (shape-drawer/sector! shape-drawer center-x center-y radius start-angle degree color))

  (draw-rectangle [_ x y w h color]
    (shape-drawer/rectangle! shape-drawer x y w h color))

  (draw-filled-rectangle [_ x y w h color]
    (shape-drawer/filled-rectangle! shape-drawer x y w h color))

  (draw-line [_ [sx sy] [ex ey] color]
    (shape-drawer/line! shape-drawer sx sy ex ey color))

  (with-line-width [_ width draw-fn]
    (shape-drawer/with-line-width shape-drawer width draw-fn))

  (draw-grid [_ leftx bottomy gridw gridh cellw cellh color]
    (shape-drawer/grid! shape-drawer color))

  (draw-on-world-view! [this f]
    (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
    (.setProjectionMatrix batch (camera/combined (:camera world-viewport)))
    (.begin batch)
    (shape-drawer/with-line-width shape-drawer world-unit-scale
      (fn []
        ; could pass new 'g' with assoc :unit-scale -> but using ctx/graphics accidentally
        ; -> icon is drawn at too big ! => mutable field.
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1)))
    (.end batch))

  (set-cursor! [_ cursor-key]
    (gdx/set-cursor! (utils/safe-get cursors cursor-key)))

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
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ tile-size))]
    (map->Graphics
     {:batch batch
      :shape-drawer-texture shape-drawer-texture
      :shape-drawer (shape-drawer/create batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
      :cursors (utils/mapvals
                (fn [[file [hotspot-x hotspot-y]]]
                  (let [pixmap (graphics/pixmap (str "cursors/" file ".png"))
                        cursor (gdx/cursor pixmap hotspot-x hotspot-y)]
                    (dispose! pixmap)
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
