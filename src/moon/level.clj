(ns moon.level
  (:require [data.grid2d :as g]
            [forge.db :as db]
            [gdl.tiled :as t]))

(defmulti generate* (fn [world] (:world/generator world)))

(defn generate [world-id]
  (let [prop (db/get world-id)]
    (assoc (generate* prop)
           :world/player-creature
           (:world/player-creature prop))))

; TODO performance bottleneck -> every time getting same layers
; takes 600 ms to read movement-properties
; lazy seqs??

(defn- tile-movement-property [tiled-map layer position]
  (let [value (t/property-value tiled-map layer position :movement)]
    (assert (not= value :undefined)
            (str "Value for :movement at position "
                 position  " / mapeditor inverted position: " [(position 0)
                                                               (- (dec (t/height tiled-map))
                                                                  (position 1))]
                 " and layer " (t/layer-name layer) " is undefined."))
    (when-not (= :no-cell value)
      value)))

(defn- movement-property-layers [tiled-map]
  (filter #(t/get-property % :movement-properties)
          (reverse
           (t/layers tiled-map))))

(defn movement-properties [tiled-map position]
  (for [layer (movement-property-layers tiled-map)]
    [(t/layer-name layer)
     (tile-movement-property tiled-map layer position)]))

(defn movement-property [tiled-map position]
  (or (->> tiled-map
           movement-property-layers
           (some #(tile-movement-property tiled-map % position)))
      "none"))
