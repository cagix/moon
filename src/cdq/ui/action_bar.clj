(ns cdq.ui.action-bar
  (:require [cdq.info :as info]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn create []
  (ui/table {:rows [[{:actor (doto (ui/horizontal-group {:pad 2 :space 2})
                               (actor/set-user-object! ::horizontal-group)
                               (group/add-actor! (doto (actor/create {})
                                                   (actor/set-name! "button-group")
                                                   (actor/set-user-object! (ui/button-group {:max-check-count 1
                                                                                             :min-check-count 0})))))
                      :expand? true
                      :bottom? true}]]
             :id ::action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- get-data [stage]
  (let [group (::horizontal-group (::action-bar-table stage))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "button-group"))}))

(defn selected-skill [stage]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-data stage)))]
    (actor/user-object skill-button)))

(defn add-skill! [stage {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-data stage)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-user-object! button id)
    (ui/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn remove-skill! [stage {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-data stage)
        button (get horizontal-group id)]
    (actor/remove! button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))
