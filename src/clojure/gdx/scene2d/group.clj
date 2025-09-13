(ns clojure.gdx.scene2d.group
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn add! [group actor]
  (Group/.addActor group actor))

(defn find-actor [^Group group name]
  (.findActor group name))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))
