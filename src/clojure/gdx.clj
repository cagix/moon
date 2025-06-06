(ns clojure.gdx
  (:require [clojure.gdx.input.buttons :as input.buttons]
            [clojure.gdx.input.keys :as input.keys]
            [clojure.gdx.graphics.color :as color]
            [gdl.tiled :as tiled]
            [gdl.utils.disposable :as disposable])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color
                                      Colors)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.maps MapProperties)
           (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TiledMapTileLayer$Cell
                                        TmxMapLoader)
           (com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile)
           (com.badlogic.gdx.utils Align
                                   ScreenUtils)))

(defn- static-field [mapping exception-name k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown " exception-name ": " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))

(def k->input-button (partial static-field input.buttons/mapping "Button"))
(def k->input-key    (partial static-field input.keys/mapping    "Key"))
(def k->color        (partial static-field color/mapping         "Color"))

(defn- create-color
  ([r g b]
   (create-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn ->color ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (k->color c)
        (vector? c) (apply create-color c)
        :else (throw (ex-info "Cannot understand color" c))))

(defn add-markdown-color! [name color]
  (Colors/put name (->color color)))

(defn k->align
  "Returns the `com.badlogic.gdx.utils.Align` enum for keyword `k`.

  `k` is either `:center`, `:left` or `:right` and `Align` value is `Align/center`, `Align/left` and `Align/right`."
  [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn clear-screen! []
  (ScreenUtils/clear Color/BLACK))

(defprotocol GetMapProperties
  (get-map-properties ^MapProperties [_]))

(extend-protocol GetMapProperties
  TiledMap          (get-map-properties [this] (.getProperties this))
  TiledMapTileLayer (get-map-properties [this] (.getProperties this)))

(defn- build-map-properties [obj]
  (let [ps (get-map-properties obj)]
    (zipmap (.getKeys ps) (.getValues ps))))

(defn- add-map-properties! [has-map-properties properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put (get-map-properties has-map-properties) k v)))

(defn- reify-tiled-layer [^TiledMapTileLayer this]
  (reify
    ILookup
    (valAt [_ key]
      (.get (.getProperties this) key))

    tiled/HasMapProperties
    (map-properties [_]
      (build-map-properties this))

    tiled/TiledMapTileLayer
    (set-visible! [_ boolean]
      (.setVisible this boolean))

    (visible? [_]
      (.isVisible this))

    (layer-name [_]
      (.getName this))

    (tile-at [_ [x y]]
      (when-let [cell (.getCell this x y)]
        (.getTile cell)))

    (property-value [_ [x y] property-key]
      (if-let [cell (.getCell this x y)]
        (if-let [value (.get (.getProperties (.getTile cell)) property-key)]
          value
          :undefined)
        :no-cell))))

(defn- add-tiled-map-tile-layer!
  "Returns nil."
  [^TiledMap tiled-map
   {:keys [name
           visible?
           properties
           tiles]}]
  {:pre [(string? name)
         (boolean? visible?)]}
  (let [tm-props (.getProperties tiled-map)
        layer (TiledMapTileLayer. (.get tm-props "width")
                                  (.get tm-props "height")
                                  (.get tm-props "tilewidth")
                                  (.get tm-props "tileheight"))]
    (.setName layer name)
    (.setVisible layer visible?)
    (add-map-properties! layer properties)
    (doseq [[[x y] tiled-map-tile] tiles
            :when tiled-map-tile]
      (.setCell layer x y (doto (TiledMapTileLayer$Cell.)
                            (.setTile tiled-map-tile))))
    (.add (.getLayers tiled-map) layer)
    nil))

(defn- reify-tiled-map [^TiledMap this]
  (reify
    disposable/Disposable
    (dispose! [_]
      (.dispose this))

    ILookup
    (valAt [_ key]
      (case key
        :tiled-map/java-object this
        :tiled-map/width  (.get (.getProperties this) "width")
        :tiled-map/height (.get (.getProperties this) "height")))

    tiled/HasMapProperties
    (map-properties [_]
      (build-map-properties this))

    tiled/TiledMap
    (layers [_]
      (map reify-tiled-layer (.getLayers this)))

    (layer-index [_ layer]
      (let [idx (.getIndex (.getLayers this) ^String (tiled/layer-name layer))]
        (when-not (= idx -1)
          idx)))

    (get-layer [_ layer-name]
      (reify-tiled-layer (.get (.getLayers this) ^String layer-name)))

    (add-layer! [_ layer-declaration]
      (add-tiled-map-tile-layer! this layer-declaration))))

(defn tmx-tiled-map
  "Has to be disposed because it loads textures.
  Loads through internal file handle."
  [file-name]
  (reify-tiled-map
   (.load (TmxMapLoader.) file-name)))

(defn create-tiled-map [{:keys [properties
                                layers]}]
  (let [tiled-map (TiledMap.)]
    (add-map-properties! tiled-map properties)
    (doseq [layer layers]
      (add-tiled-map-tile-layer! tiled-map layer))
    (reify-tiled-map tiled-map)))

; why this ? can't I just reuse them?
; probably something with dispose old maps and lose texture resources associated with it
(def copy-tile
  "Memoized function.
  Tiles are usually shared by multiple cells.
  https://libgdx.com/wiki/graphics/2d/tile-maps#cells
  No copied-tile for AnimatedTiledMapTile yet (there was no copy constructor/method)"
  (memoize
   (fn [^StaticTiledMapTile tile]
     (assert tile)
     (StaticTiledMapTile. tile))))

; probably memoize over texture-region data not the java obj itself
; -> memory leak when creating too many maps
(defn static-tiled-map-tile [texture-region property-name property-value]
  {:pre [(:texture-region/java-object texture-region)
         (string? property-name)]}
  (let [tile (StaticTiledMapTile. ^TextureRegion (:texture-region/java-object texture-region))]
    (.put (.getProperties tile) property-name property-value)
    tile))
