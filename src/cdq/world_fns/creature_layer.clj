(ns cdq.world-fns.creature-layer
  (:require [com.badlogic.gdx.maps.tiled.tiles :as tiles]
            [gdl.tiled :as tiled]))

; out of memory error -> each texture region is a new object
; so either memoize on id or property/image already calculated !? idk
(def ^:private creature-tile
  (memoize
   (fn [{:keys [tile/id
                tile/texture-region]}]
     (assert (and id
                  texture-region))
     (tiles/static-tiled-map-tile texture-region "id" id))))

(defn add-creatures-layer! [tiled-map spawn-positions]
  (tiled/add-layer! tiled-map {:name "creatures"
                               :visible? false
                               :tiles (for [[position creature-property] spawn-positions]
                                        [position (creature-tile creature-property)])}))
