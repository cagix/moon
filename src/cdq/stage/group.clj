(ns cdq.stage.group
  (:require [cdq.stage.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn children [^Group group]
  (.getChildren group))

(defn clear-children! [^Group group]
  (.clearChildren group))

(defn find-actor [^Group group actor-name]
  (.findActor group actor-name))

(defn add-actor! [^Group group actor]
  (.addActor group actor))

(defn find-actor-with-id [^Group group id]
  (let [actors (children group)
        ids (keep actor/user-object actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (actor/user-object %)) actors))))

(defmacro proxy-ILookup
  "For actors inheriting from Group."
  [class args]
  `(proxy [~class clojure.lang.ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))
