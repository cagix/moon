(ns forge.entity.impl
  (:require [forge.entity :as entity]
            [forge.entity.components :refer [remove-mods]]
            [forge.world :refer [audiovisual stopped?]]))

(defmethod entity/->v :entity/hp   [[_ v]] [v v])
(defmethod entity/->v :entity/mana [[_ v]] [v v])

(defmethod entity/tick :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)
    (swap! eid remove-mods modifiers)))

(defmethod entity/tick :entity/string-effect [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)))

(defmethod entity/destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (audiovisual (:position @eid) (build audiovisuals-id)))
