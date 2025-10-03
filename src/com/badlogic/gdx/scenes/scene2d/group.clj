(ns com.badlogic.gdx.scenes.scene2d.group
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn add! [^Group group actor]
  (.addActor group actor))

(defn find-actor [^Group group name]
  (.findActor group name))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))

(defn create []
  (Group.))
