(ns anvil.widgets.action-bar
  (:require [gdl.info :as info]
            [cdq.context :as world]
            [gdl.ui :refer [add-tooltip!] :as ui]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :refer [add-actor!]]
            [clojure.gdx.scene2d.ui.button-group :as button-group]))

(defn action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (world/get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-id button id)
    (add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (button-group/add button-group button)
    nil))

(defn action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (world/get-action-bar c)
        button (get horizontal-group id)]
    (actor/remove button)
    (button-group/remove button-group button)
    nil))
