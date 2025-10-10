(ns cdq.graphics.draw.filled-ellipse
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [x y] radius-x radius-y color]
  (sd/filled-ellipse! shape-drawer [x y] radius-x radius-y color))
