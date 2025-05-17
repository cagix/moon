(ns cdq.ui.action-bar
  (:require [cdq.info :as info]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.button-group :as button-group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn create []
  (ui/table {:rows [[{:actor (doto (ui/horizontal-group {:pad 2 :space 2})
                               (Actor/.setUserObject ::horizontal-group)
                               (Group/.addActor (doto (proxy [Actor] [])
                                                  (Actor/.setName "button-group")
                                                  (Actor/.setUserObject (button-group/create {:max-check-count 1
                                                                                              :min-check-count 0})))))
                      :expand? true
                      :bottom? true}]]
             :id ::action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- get-data [stage]
  (let [group (::horizontal-group (::action-bar-table stage))]
    {:horizontal-group group
     :button-group (Actor/.getUserObject (Group/.findActor group "button-group"))}))

(defn selected-skill [stage]
  (when-let [skill-button (button-group/checked (:button-group (get-data stage)))]
    (Actor/.getUserObject skill-button)))

(defn add-skill! [stage {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-data stage)
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (actor/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (Group/.addActor horizontal-group button)
    (button-group/add! button-group button)
    nil))

(defn remove-skill! [stage {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-data stage)
        button (get horizontal-group id)]
    (Actor/.remove button)
    (button-group/remove! button-group button)
    nil))
