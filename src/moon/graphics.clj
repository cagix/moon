(ns moon.graphics
  (:require [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.text :as text]
            [gdl.graphics.tiled :as tiled]
            [gdl.utils :refer [dispose safe-get mapvals]]
            [moon.assets :as assets]
            [moon.graphics.shape-drawer :as sd]
            [moon.graphics.world-view :as world-view])
  (:import (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)))

(declare batch)

(def ^:private ^:dynamic *unit-scale* 1)

(defn- render-view! [{:keys [viewport unit-scale]} draw-fn]
  (batch/draw-on batch
                 viewport
                 (fn []
                   (sd/with-line-width unit-scale
                     #(binding [*unit-scale* unit-scale]
                        (draw-fn))))))

(defn render-world-view! [render-fn] (render-view! world-view/view render-fn))

(defn- tr-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn ->texture-region
  ([path-or-texture]
   (let [^Texture tex (if (string? path-or-texture)
                        (get assets/manager path-or-texture)
                        path-or-texture)]
     (TextureRegion. tex)))

  ([^TextureRegion texture-region [x y w h]]
   (TextureRegion. texture-region (int x) (int y) (int w) (int h))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- unit-dimensions [image]
  (if (= *unit-scale* 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (tr-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions (world-view/unit-scale)))))

(defn draw-image [{:keys [texture-region color] :as image} position]
  (batch/draw-texture-region batch
                             texture-region
                             position
                             (unit-dimensions image)
                             0 ; rotation
                             color))

(defn draw-rotated-centered-image
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image)]
    (batch/draw-texture-region batch
                               texture-region
                               [(- (float x) (/ (float w) 2))
                                (- (float y) (/ (float h) 2))]
                               [w h]
                               rotation
                               color)))

(defn draw-centered-image [image position]
  (draw-rotated-centered-image image 0 position))

(defn- image* [texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1) ; = scale 1
      map->Sprite))

(defn image [file]
  (image* (->texture-region file)))

(defn sub-image [{:keys [texture-region]} bounds]
  (image* (->texture-region texture-region bounds)))

(defn sprite-sheet [file tilew tileh]
  {:image (image file)
   :tilew tilew
   :tileh tileh})

(defn sprite
  "x,y index starting top-left"
  [{:keys [image tilew tileh]} [x y]]
  (sub-image image [(* x tilew) (* y tileh) tilew tileh]))

(defn edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite (sprite-sheet file tilew tileh)
              [(int (/ sprite-x tilew))
               (int (/ sprite-y tileh))]))
    (image file)))

(defn- ->default-font [true-type-font]
  (or (and true-type-font (text/truetype-font true-type-font))
      (text/default-font)))

(declare ^:private default-font)

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [x y text font h-align up? scale] :as opts}]
  (text/draw (or font default-font) *unit-scale* batch opts))

(defn- ->cursors [cursors]
  (mapvals (fn [[file hotspot]]
             (graphics/cursor (str "cursors/" file ".png") hotspot))
           cursors))

(declare ^:private cursors)

(defn set-cursor! [cursor-key]
  (graphics/set-cursor (safe-get cursors cursor-key)))

(declare ^:private cached-map-renderer)

(defn draw-tiled-map
  "Renders tiled-map using world-view at world-camera position and with world-unit-scale.

  Color-setter is a `(fn [color x y])` which is called for every tile-corner to set the color.

  Can be used for lights & shadows.

  Renders only visible layers."
  [tiled-map color-setter]
  (tiled/render (cached-map-renderer tiled-map)
                color-setter
                (world-view/camera)
                tiled-map))

(defn- tiled-renderer [tiled-map]
  (tiled/renderer tiled-map (world-view/unit-scale) batch))

(defn load! [{:keys [views default-font cursors]}]
  (bind-root #'batch (SpriteBatch.))
  (bind-root #'cursors (->cursors cursors))
  (bind-root #'default-font (->default-font default-font))
  (bind-root #'cached-map-renderer (memoize tiled-renderer)))

(defn dispose! []
  (dispose batch)
  (dispose default-font)
  (run! dispose (vals cursors)))
