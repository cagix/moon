(ns com.badlogic.gdx.scenes.scene2d.group
  (:require [gdl.scene2d.group])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(extend-type Group
  gdl.scene2d.group/Group
  (add! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name))

  (clear-children! [group]
    (.clearChildren group))

  (children [group]
    (.getChildren group)))

(defn create []
  (Group.))
