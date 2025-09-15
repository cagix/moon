(ns cdq.ctx.world
  (:require [clojure.gdx.utils.disposable :as disposable]))

(def active-eids :world/active-entities)

(defn dispose! [{:keys [world/tiled-map]}]
  (disposable/dispose! tiled-map))
