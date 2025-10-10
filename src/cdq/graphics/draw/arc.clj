(ns cdq.graphics.draw.arc
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [center-x center-y] radius start-angle degree color]
  (sd/arc! shape-drawer [center-x center-y] radius start-angle degree color))
