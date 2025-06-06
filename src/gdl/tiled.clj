(ns gdl.tiled
  (:require [clojure.gdx.maps.map-properties :as map-properties]
            [clojure.gdx.maps.tiled.tiled-map :as tiled-map]
            [clojure.gdx.maps.tiled.tiles.static-tiled-map-tile :as static-tiled-map-tile])
  (:import (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TmxMapLoader)
           (com.badlogic.gdx.utils Disposable)))

(def copy-tile
  "Memoized function. Copies the given [[static-tiled-map-tile]].

  Tiles are usually shared by multiple cells, see: https://libgdx.com/wiki/graphics/2d/tile-maps#cells"
  (memoize static-tiled-map-tile/copy))

(defn static-tiled-map-tile
  "Creates a `StaticTiledMapTile` with the given `texture-region` and property."
  [texture-region property-name property-value]
  {:pre [(:texture-region/java-object texture-region)
         (string? property-name)]}
  (static-tiled-map-tile/create (:texture-region/java-object texture-region)
                                property-name
                                property-value))

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

(defn- reify-tiled-layer [^TiledMapTileLayer this]
  (reify
    clojure.lang.ILookup
    (valAt [_ key]
      (.get (.getProperties this) key))

    HasMapProperties
    (map-properties [_]
      (map-properties/->clj-map (.getProperties this)))

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

(defn- reify-tiled-map [^TiledMap this]
  (reify
    Disposable
    (dispose [_]
      (.dispose this))

    clojure.lang.ILookup
    (valAt [_ key]
      (case key
        :tiled-map/java-object this
        :tiled-map/width  (.get (.getProperties this) "width")
        :tiled-map/height (.get (.getProperties this) "height")))

    HasMapProperties
    (map-properties [_]
      (map-properties/->clj-map (.getProperties this)))

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
      (tiled-map/add-layer! this layer-declaration))))

(defn tmx-tiled-map
  "Has to be disposed because it loads textures.
  Loads through internal file handle."
  [file-name]
  (reify-tiled-map
   (.load (TmxMapLoader.) file-name)))

(defn create-tiled-map [{:keys [properties
                                layers]}]
  (let [tiled-map (TiledMap.)]
    (map-properties/add! (.getProperties tiled-map) properties)
    (doseq [layer layers]
      (tiled-map/add-layer! tiled-map layer))
    (reify-tiled-map tiled-map)))
