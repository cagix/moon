(ns cdq.ui.action-bar
  (:require [cdq.info :as info]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn- button-group []
  (doto (actor/create {})
    (.setName "action-bar/button-group")
    (.setUserObject (ui/button-group {:max-check-count 1
                                      :min-check-count 0}))))

(defn- horizontal-group []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (.addActor group (button-group))
    group))

(defn create []
  (ui/table {:rows [[{:actor (horizontal-group)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn get-data [stage]
  (let [group (:ui/action-bar (:action-bar-table stage))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "action-bar/button-group"))}))

(defn selected-skill [data]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group data))]
    (actor/user-object skill-button)))

(defn add-skill! [data {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} data
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-user-object! button id)
    (ui/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn remove-skill! [data {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} data
        button (get horizontal-group id)]
    (actor/remove! button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))
