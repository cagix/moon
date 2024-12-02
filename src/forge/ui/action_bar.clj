(ns forge.ui.action-bar
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(def ^:private image-scale 2)

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (.setUserObject actor (button-group {:max-check-count 1 :min-check-count 0}))
    actor))

(defn- group->button-group [group]
  (.getUserObject (.findActor group "action-bar/button-group")))

(defn- get-action-bar []
  (let [group (::action-bar (:action-bar-table (screen-stage)))]
    {:horizontal-group group
     :button-group (group->button-group group)}))

(defn add-skill [{:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        button (image-button image (fn []) {:scale image-scale})]
    (.setUserObject button id)
    (add-tooltip! button #(info-text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (bg-add! button-group button)
    nil))

(defn remove-skill [{:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        button (get horizontal-group id)]
    (.remove button)
    (bg-remove! button-group button)
    nil))

(defn create []
  (let [group (horizontal-group {:pad 2 :space 2})]
    (.setUserObject group ::action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn selected-skill []
  (when-let [skill-button (bg-checked (:button-group (get-action-bar)))]
    (.getUserObject skill-button)))
