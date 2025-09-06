(ns cdq.ui.group
  (:require [cdq.construct]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn add! [group actor-or-decl]
  (group/add! group (cdq.construct/construct? actor-or-decl)))

(def find-actor         group/find-actor)
(def clear-children!    group/clear-children!)
(def children           group/children)
(def find-actor-with-id group/find-actor-with-id)

(defn set-opts! [group opts]
  (run! (partial add! group) (:actors opts))
  group)
