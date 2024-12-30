(ns anvil.widgets.action-bar
  (:require [anvil.info :as info]
            [cdq.context :as world]
            [gdl.ui :refer [add-tooltip!] :as ui]
            [gdl.ui.group :refer [add-actor!]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (world/get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (world/get-action-bar c)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))
