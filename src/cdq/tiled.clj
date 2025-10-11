(ns cdq.tiled
  (:require [clojure.gdx.maps.map-properties :as properties]
            [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.gdx.maps.tiled.layer :as layer]
            [clojure.gdx.maps.tiled.tiles :as tiles])
  (:import (com.badlogic.gdx.maps.tiled TiledMap
                                        TiledMapTileLayer
                                        TiledMapTileLayer$Cell)))

(defprotocol HasMapProperties
  (map-properties [_]))

(defprotocol TMap
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

(extend-type TiledMapTileLayer
  HasMapProperties
  (map-properties [layer]
    (properties/->clj (layer/properties layer)))

  TMapLayer
  (set-visible! [layer boolean]
    (layer/set-visible! layer boolean))

  (visible? [layer]
    (layer/visible? layer))

  (layer-name [layer]
    (layer/name layer))

  (tile-at [layer position]
    (when-let [cell (layer/cell layer position)]
      (.getTile cell)))

  (property-value [layer position property-key]
    (if-let [cell (layer/cell layer position)]
      (if-let [value (.get (.getProperties (.getTile cell)) property-key)]
        value
        :undefined)
      :no-cell)))

(defn- create-layer
  [{:keys [width
           height
           tilewidth
           tileheight
           name
           visible?
           map-properties
           tiles]}]
  {:pre [(string? name)
         (boolean? visible?)]}
  (let [layer (doto (layer/create width height tilewidth tileheight)
                (layer/set-name! name)
                (layer/set-visible! visible?))]
    (.putAll (layer/properties layer) map-properties)
    (doseq [[position tiled-map-tile] tiles
            :when tiled-map-tile]
      (layer/set-cell! layer
                       position
                       (doto (TiledMapTileLayer$Cell.)
                         (.setTile tiled-map-tile))))
    layer))

(defn- tm-add-layer!
  "Returns nil."
  [^TiledMap tiled-map
   {:keys [name
           visible?
           properties
           tiles]}]
  (let [props (tiled-map/properties tiled-map)
        layer (create-layer {:width      (.get props "width")
                             :height     (.get props "height")
                             :tilewidth  (.get props "tilewidth")
                             :tileheight (.get props "tileheight")
                             :name name
                             :visible? visible?
                             :map-properties (properties/create properties)
                             :tiles tiles})]
    (.add (tiled-map/layers tiled-map) layer))
  nil)

(extend-type TiledMap
  HasMapProperties
  (map-properties [this]
    (properties/->clj (tiled-map/properties this)))

  TMap
  (get-layer [this layer-name]
    (.get (tiled-map/layers this) ^String layer-name))

  (add-layer! [this layer-declaration]
    (tm-add-layer! this layer-declaration)))

(defn create-tiled-map [{:keys [properties
                                layers]}]
  (let [tiled-map (tiled-map/create)]
    (properties/add! (tiled-map/properties tiled-map) properties)
    (doseq [layer layers]
      (tm-add-layer! tiled-map layer))
    tiled-map))

(def copy-tile (memoize tiles/copy))
(def static-tiled-map-tile tiles/static-tiled-map-tile)

(defn map-positions
  "Returns a sequence of all `[x y]` positions in the `tiled-map`."
  [tiled-map]
  (for [x (range (.get (tiled-map/properties tiled-map) "width"))
        y (range (.get (tiled-map/properties tiled-map) "height"))]
    [x y]))

(defn positions-with-property
  "Returns a sequence of `[[x y] value]` for all tiles who have `property-key`."
  [tiled-map layer-name property-key]
  {:pre [tiled-map
         (string? layer-name)
         (string? property-key)]}
  (let [layer (get-layer tiled-map layer-name)]
    (for [position (map-positions tiled-map)
          :let [value (property-value layer position property-key)]
          :when (not (#{:undefined :no-cell} value))]
      [position value])))

(defn- tile-movement-property [tiled-map layer position]
  (let [value (property-value layer position "movement")]
    (assert (not= value :undefined)
            (str "Value for :movement at position "
                 position  " / mapeditor inverted position: " [(position 0)
                                                               (- (dec (.get (tiled-map/properties tiled-map) "height"))
                                                                  (position 1))]
                 " and layer " (layer-name layer) " is undefined."))
    (when-not (= :no-cell value)
      value)))

(defn- movement-property-layers [tiled-map]
  (->> tiled-map
       tiled-map/layers
       reverse
       (filter #(.get (layer/properties %) "movement-properties"))))

(defn movement-properties [tiled-map position]
  (for [layer (movement-property-layers tiled-map)]
    [(layer-name layer)
     (tile-movement-property tiled-map layer position)]))

(defn movement-property [tiled-map position]
  (or (->> tiled-map
           movement-property-layers
           (some #(tile-movement-property tiled-map % position)))
      "none"))
