(ns cdq.graphics.draw.filled-rectangle
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} x y w h color]
  (sd/filled-rectangle! shape-drawer x y w h color))
