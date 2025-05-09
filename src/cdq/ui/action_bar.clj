(ns cdq.ui.action-bar
  (:require [cdq.info :as info]
            [clojure.gdx.scene2d.ui :as ui :refer [ui-actor]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn- button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (.setUserObject actor (ui/button-group {:max-check-count 1
                                            :min-check-count 0}))
    actor))

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
     :button-group (Actor/.getUserObject (Group/.findActor group "action-bar/button-group"))}))

(defn selected-skill [data]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group data))]
    (Actor/.getUserObject skill-button)))

(defn add-skill! [data {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} data
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (ui/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (Group/.addActor horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn remove-skill! [data {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} data
        button (get horizontal-group id)]
    (Actor/.remove button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))
