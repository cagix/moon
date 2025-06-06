(ns cdq.utils.tiled
  (:require [gdl.tiled :as tiled]))

(defn- map-positions
  "Returns a sequence of all `[x y]` positions in the `tiled-map`."
  [tiled-map]
  (for [x (range (:tiled-map/width  tiled-map))
        y (range (:tiled-map/height tiled-map))]
    [x y]))

(defn positions-with-property
  "Returns a sequence of `[[x y] value]` for all tiles who have `property-key`."
  [tiled-map layer-name property-key]
  {:pre [tiled-map
         (string? layer-name)
         (string? property-key)]}
  (let [layer (tiled/get-layer tiled-map layer-name)]
    (for [position (map-positions tiled-map)
          :let [value (tiled/property-value layer position property-key)]
          :when (not (#{:undefined :no-cell} value))]
      [position value])))

(defn- tile-movement-property [tiled-map layer position]
  (let [value (tiled/property-value layer position "movement")]
    (assert (not= value :undefined)
            (str "Value for :movement at position "
                 position  " / mapeditor inverted position: " [(position 0)
                                                               (- (dec (:tiled-map/height tiled-map))
                                                                  (position 1))]
                 " and layer " (tiled/layer-name layer) " is undefined."))
    (when-not (= :no-cell value)
      value)))

(defn- movement-property-layers [tiled-map]
  (->> tiled-map
       tiled/layers
       reverse
       (filter #(get % "movement-properties"))))

(defn movement-properties [tiled-map position]
  (for [layer (movement-property-layers tiled-map)]
    [(tiled/layer-name layer)
     (tile-movement-property tiled-map layer position)]))

(defn movement-property [tiled-map position]
  (or (->> tiled-map
           movement-property-layers
           (some #(tile-movement-property tiled-map % position)))
      "none"))
