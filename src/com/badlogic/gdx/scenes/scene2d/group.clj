(ns com.badlogic.gdx.scenes.scene2d.group
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defprotocol PGroup
  (add! [group actor])
  (find-actor [group name])
  (clear-children! [group])
  (children [group])
  (set-opts! [_ opts]))

(extend-type Group
  PGroup
  (add! [group actor]
    (.addActor group actor))

  (find-actor [group name]
    (.findActor group name))

  (clear-children! [group]
    (.clearChildren group))

  (children [group]
    (.getChildren group))

  (set-opts! [group opts]
    (run! (fn [actor-or-decl]
            (add! group (if (instance? Actor actor-or-decl)
                          actor-or-decl
                          (scene2d/build actor-or-decl))))
          (:group/actors opts))
    (actor/set-opts! group opts)))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (Group.)
    (set-opts! opts)))
