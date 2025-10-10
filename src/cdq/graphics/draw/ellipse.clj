(ns cdq.graphics.draw.ellipse
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
  (sd/ellipse! shape-drawer [x y] radius-x radius-y color))
