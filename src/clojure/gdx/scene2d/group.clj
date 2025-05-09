(ns clojure.gdx.scene2d.group
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn children [^Group group]
  (.getChildren group))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn find-actor [^Group group actor-name]
  (.findActor group actor-name))
