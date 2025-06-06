(ns gdl.tiled
  (:require [clojure.gdx.maps.map-properties :as map-properties]
            [clojure.gdx.maps.tiled.tiled-map :as tiled-map]
            [clojure.gdx.maps.tiled.tiled-map-tile-layer :as layer]
            [clojure.gdx.maps.tiled.tiles.static-tiled-map-tile :as static-tiled-map-tile]
            [clojure.gdx.maps.tiled.tmx-map-loader :as tmx-map-loader]
            [gdl.utils.disposable :as disposable]))

(defn- tm-add-layer!
  "Returns nil."
  [tiled-map {:keys [name
                     visible?
                     properties
                     tiles]}]
  (let [props (tiled-map/properties tiled-map)
        layer (layer/create {:width      (.get props "width")
                             :height     (.get props "height")
                             :tilewidth  (.get props "tilewidth")
                             :tileheight (.get props "tileheight")
                             :name name
                             :visible? visible?
                             :map-properties (map-properties/create properties)
                             :tiles tiles})]
    (.add (tiled-map/layers tiled-map) layer))
  nil)

(def copy-tile
  "Memoized function. Copies the given [[static-tiled-map-tile]].

  Tiles are usually shared by multiple cells, see: https://libgdx.com/wiki/graphics/2d/tile-maps#cells"
  (memoize static-tiled-map-tile/copy))

(defn static-tiled-map-tile
  "Creates a `StaticTiledMapTile` with the given `texture-region` and property."
  [texture-region property-name property-value]
  {:pre [texture-region
         (string? property-name)]}
  (static-tiled-map-tile/create texture-region property-name property-value))

(defprotocol HasMapProperties
  (map-properties [_]
                  "Returns the map-properties of the given tiled-map or tiled-map-layer as clojure map."))

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

(defn- reify-tiled-layer [this]
  (reify
    clojure.lang.ILookup
    (valAt [_ key]
      (.get (layer/properties this) key))

    HasMapProperties
    (map-properties [_]
      (map-properties/->clj-map (layer/properties this)))

    TMapLayer
    (set-visible! [_ boolean]
      (layer/set-visible! this boolean))

    (visible? [_]
      (layer/visible? this))

    (layer-name [_]
      (layer/name this))

    (tile-at [_ position]
      (when-let [cell (layer/get-cell this position)]
        (.getTile cell)))

    (property-value [_ position property-key]
      (if-let [cell (layer/get-cell this position)]
        (if-let [value (.get (.getProperties (.getTile cell)) property-key)]
          value
          :undefined)
        :no-cell))))

(defn- reify-tiled-map [this]
  (reify
    disposable/Disposable
    (dispose! [_]
      (tiled-map/dispose! this))

    clojure.lang.ILookup
    (valAt [_ key]
      (case key
        :tiled-map/java-object this
        :tiled-map/width  (.get (tiled-map/properties this) "width")
        :tiled-map/height (.get (tiled-map/properties this) "height")))

    HasMapProperties
    (map-properties [_]
      (map-properties/->clj-map (tiled-map/properties this)))

    TMap
    (layers [_]
      (map reify-tiled-layer (tiled-map/layers this)))

    (layer-index [_ layer]
      (let [idx (.getIndex (tiled-map/layers this) ^String (layer-name layer))]
        (when-not (= idx -1)
          idx)))

    (get-layer [_ layer-name]
      (reify-tiled-layer (.get (tiled-map/layers this) ^String layer-name)))

    (add-layer! [_ layer-declaration]
      (tm-add-layer! this layer-declaration))))

(defn tmx-tiled-map
  "Has to be disposed because it loads textures.
  Loads through internal file handle."
  [file-name]
  (-> file-name
      tmx-map-loader/load!
      reify-tiled-map))

(defn create-tiled-map [{:keys [properties
                                layers]}]
  (let [tiled-map (tiled-map/create)]
    (map-properties/add! (tiled-map/properties tiled-map) properties)
    (doseq [layer layers]
      (tm-add-layer! tiled-map layer))
    (reify-tiled-map tiled-map)))
