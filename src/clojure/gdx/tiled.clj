(ns clojure.gdx.tiled
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.maps MapProperties)
           (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TiledMapTileLayer$Cell)
           (com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile)))

(defn map-properties->clj-map [^MapProperties mp]
  (zipmap (.getKeys   mp)
          (.getValues mp)))

(defn add-map-properties!
  "properties is a clojure map of string keys to values which get added to the `MapProperties`"
  [^MapProperties mp properties]
  (doseq [[k v] properties]
    (assert (string? k))
    (.put mp k v)))

(defn copy-static-tiled-map-tile [^StaticTiledMapTile tile]
  (assert tile)
  (StaticTiledMapTile. tile))

(defn static-tiled-map-tile [texture-region property-name property-value]
  {:pre [texture-region
         (string? property-name)]}
  (let [tile (StaticTiledMapTile. ^TextureRegion texture-region)]
    (.put (.getProperties tile) property-name property-value)
    tile))

(defn add-tiled-map-tile-layer!
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
    (add-map-properties! (.getProperties layer) properties)
    (doseq [[[x y] tiled-map-tile] tiles
            :when tiled-map-tile]
      (.setCell layer x y (doto (TiledMapTileLayer$Cell.)
                            (.setTile tiled-map-tile))))
    (.add (.getLayers tiled-map) layer)
    nil))
