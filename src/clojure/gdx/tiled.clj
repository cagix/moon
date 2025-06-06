(ns clojure.gdx.tiled)

(defprotocol GetMapProperties
  (get-map-properties ^MapProperties [_]
                      "Internal helper for building map properties for TiledMap/Layer instances.
                      (There is no common interface)."))

(extend-protocol GetMapProperties
  TiledMap          (get-map-properties [this] (.getProperties this))
  TiledMapTileLayer (get-map-properties [this] (.getProperties this)))
