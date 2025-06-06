; gdl is my minimal facade API for libgdx -> so use gdl disposable/textures here
(ns clojure.gdx.tiled
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.maps MapProperties)
           (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TiledMapTileLayer$Cell
                                        TmxMapLoader)
           (com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile)
           (com.badlogic.gdx.utils Disposable)))

(def copy-tile
  "Memoized function. Copies the given [[static-tiled-map-tile]].

  Tiles are usually shared by multiple cells, see: https://libgdx.com/wiki/graphics/2d/tile-maps#cells"
  (memoize
   (fn [^StaticTiledMapTile tile]
     (assert tile)
     (StaticTiledMapTile. tile))))

(defn static-tiled-map-tile
  "Creates a `StaticTiledMapTile` with the given `texture-region` and property."
  [texture-region property-name property-value]
  {:pre [(:texture-region/java-object texture-region) ; ??? <- these are gdl textures ...
         (string? property-name)]}
  (let [tile (StaticTiledMapTile. ^TextureRegion (:texture-region/java-object texture-region))] ; ???
    (.put (.getProperties tile) property-name property-value)
    tile))

(defprotocol HasMapProperties
  (map-properties [_]
                  "Returns the map-properties of the given tiled-map or tiled-map-layer as clojure map."))

(defprotocol GetMapProperties
  (^:private get-map-properties ^MapProperties [_]
             "Internal helper for building map properties for TiledMap/Layer instances.
             (There is no common interface)."))

(extend-protocol GetMapProperties
  TiledMap          (get-map-properties [this] (.getProperties this))
  TiledMapTileLayer (get-map-properties [this] (.getProperties this)))

(defn- build-map-properties [obj]
  (let [ps (get-map-properties obj)]
    (zipmap (.getKeys ps) (.getValues ps))))

(defprotocol TMap
  (layers [tiled-map]
          "Returns the layers of the tiled-map (instance of [[TMapLayer]]).")

  (layer-index [tiled-map layer]
               "Returns nil or the integer index of the layer.")

  (get-layer [tiled-map layer-name]
             "Returns the layer with name (string).")

  (add-layer! [tiled-map {:keys [name
                                 visible?
                                 properties
                                 tiles]}]
              "`properties` is optional. Returns nil."))

(defprotocol TMapLayer
  (tile-at [_ [x y]]
           "If a cell is defined at the position, returns the tile. Otherwise returns nil.")
  (layer-name [layer])
  (set-visible! [layer boolean])
  (visible? [layer])
  (property-value [layer [x y] property-key]
                  "Returns the property value of the tile at the cell in layer.
                  If there is no cell at this position in the layer returns `:no-cell`.
                  If the property value is undefined returns `:undefined`."))

(defn- add-map-properties! [has-map-properties properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put (get-map-properties has-map-properties) k v)))

(defn- reify-tiled-layer [^TiledMapTileLayer this]
  (reify
    ILookup
    (valAt [_ key]
      (.get (.getProperties this) key))

    HasMapProperties
    (map-properties [_]
      (build-map-properties this))

    TMapLayer
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
    Disposable
    (dispose [_]
      (.dispose this))

    ILookup
    (valAt [_ key]
      (case key
        :tiled-map/java-object this
        :tiled-map/width  (.get (.getProperties this) "width")
        :tiled-map/height (.get (.getProperties this) "height")))

    HasMapProperties
    (map-properties [_]
      (build-map-properties this))

    TMap
    (layers [_]
      (map reify-tiled-layer (.getLayers this)))

    (layer-index [_ layer]
      (let [idx (.getIndex (.getLayers this) ^String (layer-name layer))]
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
