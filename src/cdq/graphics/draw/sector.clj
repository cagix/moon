(ns cdq.graphics.draw.sector
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd/sector! shape-drawer [center-x center-y] radius start-angle degree color))
