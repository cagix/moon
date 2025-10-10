(ns cdq.graphics.draw.circle
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [x y] radius color]
  (sd/circle! shape-drawer [x y] radius color))
