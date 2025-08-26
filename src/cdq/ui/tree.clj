(ns cdq.ui.tree
  (:import (com.badlogic.gdx.scenes.scene2d.ui Tree$Node)))

(defn node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))
