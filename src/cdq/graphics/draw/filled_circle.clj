(ns cdq.graphics.draw.filled-circle
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} [x y] radius color]
  (sd/filled-circle! shape-drawer [x y] radius color))
