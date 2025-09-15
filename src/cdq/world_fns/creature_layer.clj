(ns cdq.world-fns.creature-layer
  (:require [clojure.gdx.maps.tiled.tiles.static-tiled-map-tile :as static-tiled-map-tile]
            [clojure.tiled :as tiled]))

; out of memory error -> each texture region is a new object
; so either memoize on id or property/image already calculated !? idk
(def ^:private creature-tile
  (memoize
   (fn [{:keys [tile/id
                tile/texture-region]}]
     (assert (and id
                  texture-region))
     (static-tiled-map-tile/create texture-region "id" id))))

(defn add-creatures-layer! [tiled-map spawn-positions]
  (tiled/add-layer! tiled-map {:name "creatures"
                               :visible? false
                               :tiles (for [[position creature-property] spawn-positions]
                                        [position (creature-tile creature-property)])}))
