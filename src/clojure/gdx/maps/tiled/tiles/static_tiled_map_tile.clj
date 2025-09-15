(ns clojure.gdx.maps.tiled.tiles.static-tiled-map-tile
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.maps.tiled.tiles StaticTiledMapTile)))

(def copy
  "Memoized function. Copies the given `StaticTiledMapTile`.

  Tiles are usually shared by multiple cells, see: https://libgdx.com/wiki/graphics/2d/tile-maps#cells"
  (memoize
   (fn [^StaticTiledMapTile tile]
     (assert tile)
     (StaticTiledMapTile. tile))))

(defn create
  "Creates a `StaticTiledMapTile` with the given `texture-region` and property."
  [texture-region property-name property-value]
  {:pre [texture-region
         (string? property-name)]}
  (let [tile (StaticTiledMapTile. ^TextureRegion texture-region)]
    (.put (.getProperties tile) property-name property-value)
    tile))
