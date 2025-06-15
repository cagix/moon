(ns gdx.ui.group
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn find-actor [^Group group name]
  (.findActor group name))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))
