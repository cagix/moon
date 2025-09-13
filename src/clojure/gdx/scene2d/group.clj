(ns clojure.gdx.scene2d.group
  (:require [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.actor.decl :as actor.decl])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

; Options can do it but here not
; as if we do group/add! we are still in scene2d territory working with those objects
(defn add! [group actor-or-decl]
  (Group/.addActor group (if (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
                           actor-or-decl
                           (actor.decl/build actor-or-decl))))

(defn find-actor [^Group group name]
  (.findActor group name))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn children [^Group group]
  (.getChildren group))

(defn find-actor-with-id [group id]
  (let [actors (children group)
        ids (keep actor/user-object actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/user-object %)) actors))))

(defn set-opts! [group opts]
  (run! (partial add! group) (:group/actors opts))
  (actor/set-opts! group opts))
