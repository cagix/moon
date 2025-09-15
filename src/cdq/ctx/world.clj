(ns cdq.ctx.world
  (:require [clojure.gdx.utils.disposable :as disposable]))

(def active-eids (comp :world/active-entities :ctx/world))

(defn dispose! [{:keys [world/tiled-map]}]
  (disposable/dispose! tiled-map))
