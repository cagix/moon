(ns clojure.gdx.scene2d.group
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.actor.decl :as actor.decl])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn add! [group actor]
  (Group/.addActor group actor))

(defn find-actor [^Group group name]
  (.findActor group name))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))

(defn set-opts! [group opts]
  (run! (fn [actor-or-decl]
          (add! group (if (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
                        actor-or-decl
                        (actor.decl/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))
