(ns cdq.ui.group
  (:require [cdq.ui :as ui]
            [cdq.ui.actor :as actor]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn add! [group actor-or-decl]
  (group/add! group (ui/construct? actor-or-decl)))

(def find-actor         group/find-actor)
(def clear-children!    group/clear-children!)
(def children           group/children)

(defn set-opts! [group opts]
  (run! (partial add! group) (:actors opts))
  (actor/set-opts! group opts))

(defn create [opts]
  (doto (scene2d/group)
    (set-opts! opts)))
