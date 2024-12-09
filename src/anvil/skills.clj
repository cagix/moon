(ns anvil.skills
  (:refer-clojure :exclude [contains? remove])
  (:require [anvil.info :as info]
            [anvil.stage :as stage]
            [anvil.ui :refer [ui-actor image-button add-tooltip!] :as ui]
            [clojure.gdx.scene2d.actor :refer [user-object]]
            [clojure.gdx.scene2d.group :refer [add-actor! find-actor]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (Actor/.setUserObject actor (ui/button-group {:max-check-count 1
                                                  :min-check-count 0}))
    actor))

(defn- group->button-group [group]
  (user-object (find-actor group "action-bar/button-group")))

(defn- get-action-bar []
  (let [group (::action-bar (:action-bar-table (stage/get)))]
    {:horizontal-group group
     :button-group (group->button-group group)}))

(defn- action-bar-add-skill [{:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        button (image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn- action-bar-remove-skill [{:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))

(defn action-bar []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group ::action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn selected []
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar)))]
    (user-object skill-button)))

(defn contains? [{:keys [entity/skills]} {:keys [property/id]}]
  (clojure.core/contains? skills id))

(defn add [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? entity skill))]}
  (when (:entity/player? entity)
    (action-bar-add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove [entity {:keys [property/id] :as skill}]
  {:pre [(contains? entity skill)]}
  (when (:entity/player? entity)
    (action-bar-remove-skill skill))
  (update entity :entity/skills dissoc id))
