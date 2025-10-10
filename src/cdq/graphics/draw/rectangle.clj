(ns cdq.graphics.draw.rectangle
  (:require [clojure.gdx.shape-drawer :as sd]))

(defn do! [{:keys [graphics/shape-drawer]} x y w h color]
  (sd/rectangle! shape-drawer x y w h color))
