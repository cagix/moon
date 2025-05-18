(ns cdq.ui.action-bar
  (:require [cdq.info :as info]
            [gdl.ui :as ui]
            [gdl.ui.button-group :as button-group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- button-group-container []
  (ui/actor {:name "button-group-container"
             :user-object (button-group/create {:max-check-count 1
                                                :min-check-count 0})}))

(defn- horizontal-group []
  (ui/horizontal-group {:pad 2
                        :space 2
                        :user-object :horizontal-group
                        :actors [(button-group-container)]}))

(defn create [& {:keys [id]}]
  (ui/table {:rows [[{:actor (horizontal-group)
                      :expand? true
                      :bottom? true}]]
             :id id
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- get-data [action-bar]
  (let [group (:horizontal-group action-bar)]
    {:horizontal-group group
     :button-group (Actor/.getUserObject (ui/find-actor group "button-group-container"))}))

(defn selected-skill [action-bar]
  (when-let [skill-button (button-group/checked (:button-group (get-data action-bar)))]
    (Actor/.getUserObject skill-button)))

(defn add-skill! [action-bar {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (ui/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (ui/add-actor! horizontal-group button)
    (button-group/add! button-group button)
    nil))

(defn remove-skill! [action-bar {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group id)]
    (Actor/.remove button)
    (button-group/remove! button-group button)
    nil))

(comment
 (keys (:entity/skills @cdq.ctx/player-eid))

 (remove-skill! (:action-bar cdq.ctx/stage) {:property/id :skills/spawn}))
