(ns cdq.world
  (:require [clojure.gdx.utils.disposable :as disposable]))

(defn dispose! [{:keys [world/tiled-map]}]
  (assert tiled-map) ; only dispose after world was created
  (disposable/dispose! tiled-map))

(defprotocol World
  (cache-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_]))
