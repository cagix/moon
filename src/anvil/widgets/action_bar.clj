(ns anvil.widgets.action-bar
  (:require [anvil.entity.skills :as skills]
            [anvil.info :as info]
            [anvil.widgets :as widgets]
            [cdq.context :as world]
            [gdl.ui :refer [ui-actor add-tooltip!] :as ui]
            [gdl.ui.group :refer [add-actor!]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (Actor/.setUserObject actor (ui/button-group {:max-check-count 1
                                                  :min-check-count 0}))
    actor))

(defn- action-bar []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn-impl widgets/action-bar-table [_context]
  (ui/table {:rows [[{:actor (action-bar)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (world/get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn- action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (world/get-action-bar c)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))

(bind-root skills/player-add-skill    action-bar-add-skill)
(bind-root skills/player-remove-skill action-bar-remove-skill)
