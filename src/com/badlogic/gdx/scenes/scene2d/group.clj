(ns com.badlogic.gdx.scenes.scene2d.group
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn add! [^Group group actor]
  (.addActor group actor))

(defn find-actor [^Group group name]
  (.findActor group name))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))

(defn set-opts! [^Group group opts]
  (run! (fn [actor-or-decl]
          (add! group (if (instance? Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (Group.)
    (set-opts! opts)))
