(ns cdq.ui.group
  (:require [cdq.ui.actor :as actor]
            [clojure.gdx.scenes.scene2d.actor :as scene2d.actor]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn add! [group actor-or-decl]
  (group/add! group (scene2d.actor/construct? actor-or-decl)))

(def find-actor         group/find-actor)
(def clear-children!    group/clear-children!)
(def children           group/children)

(defn set-opts! [group opts]
  (run! (partial add! group) (:actors opts))
  (actor/set-opts! group opts))

(defn create [opts]
  (doto (group/create)
    (set-opts! opts)))
