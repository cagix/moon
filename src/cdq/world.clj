(ns cdq.world
  (:import (com.badlogic.gdx.utils Disposable)))

(defn dispose! [{:keys [world/tiled-map]}]
  (assert tiled-map) ; only dispose after world was created
  (Disposable/.dispose tiled-map))

(defprotocol World
  (cache-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_]))
