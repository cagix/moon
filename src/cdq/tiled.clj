(ns cdq.tiled
  (:require [clojure.gdx.maps.map-properties :as properties]
            [clojure.gdx.maps.tiled :as tiled-map]
            [clojure.gdx.maps.tiled.layer :as layer]
            [clojure.gdx.maps.tiled.layer.cell :as cell]
            [clojure.gdx.maps.tiled.tiles :as tiles]))

(defn property-value [layer position property-key]
  (if-let [cell (layer/cell layer position)]
    (if-let [value (.get (.getProperties (.getTile cell)) property-key)]
      value
      :undefined)
    :no-cell))

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
                       (cell/create tiled-map-tile)))
    layer))

(defn add-layer!
  "`properties` is optional. Returns nil."
  [tiled-map
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

; used in 2 world fn gen stuff
(defn create-tiled-map [{:keys [properties
                                layers]}]
  (let [tiled-map (tiled-map/create)]
    (properties/add! (tiled-map/properties tiled-map) properties)
    (doseq [layer layers]
      (add-layer! tiled-map layer))
    tiled-map))

(def copy-tile (memoize tiles/copy))
(def static-tiled-map-tile tiles/static-tiled-map-tile)

(defn- tile-movement-property [tiled-map layer position]
  (when-let [cell (layer/cell layer position)]
    (let [value (-> cell
                    .getTile
                    .getProperties
                    (.get "movement"))]
      (assert value
              (str "Value for :movement at position "
                   position  " / mapeditor inverted position: " [(position 0)
                                                                 (- (dec (.get (tiled-map/properties tiled-map) "height"))
                                                                    (position 1))]
                   " and layer " (layer/name layer) " is undefined."))
      value)))

(defn- movement-property-layers [tiled-map]
  (->> tiled-map
       tiled-map/layers
       reverse
       (filter #(.get (layer/properties %) "movement-properties"))))

(defn movement-properties [tiled-map position]
  (for [layer (movement-property-layers tiled-map)]
    [(layer/name layer)
     (tile-movement-property tiled-map layer position)]))

(defn movement-property [tiled-map position]
  (or (->> tiled-map
           movement-property-layers
           (some #(tile-movement-property tiled-map % position)))
      "none"))
