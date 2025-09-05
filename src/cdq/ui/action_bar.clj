(ns cdq.ui.action-bar
  (:require [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.image-button :as image-button])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button
                                               ButtonGroup)))

(defn create [_ctx {:keys [id]}]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/horizontal-group
                    :pad 2
                    :space 2
                    :user-object :horizontal-group
                    :actors [{:actor/type :actor.type/actor
                              :name "button-group-container"
                              :user-object (doto (ButtonGroup.)
                                             (.setMaxCheckCount 1)
                                             (.setMinCheckCount 0))}]}
            :expand? true
            :bottom? true}]]
   :id id
   :cell-defaults {:pad 2}
   :fill-parent? true})

(defn- get-data [action-bar]
  (let [group (:horizontal-group action-bar)]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "button-group-container"))}))

(defn selected-skill [action-bar]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-data action-bar)))]
    (actor/user-object skill-button)))

(defn add-skill!
  [action-bar
   {:keys [skill-id
           texture-region
           tooltip-text]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (image-button/create {:texture-region texture-region
                                     :scale 2})]
    (actor/set-user-object! button skill-id)
    (actor/add-tooltip!     button tooltip-text)
    (group/add!        horizontal-group button)
    (ButtonGroup/.add  button-group     ^Button button)
    nil))

(defn remove-skill! [action-bar skill-id]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group skill-id)]
    (actor/remove!                            button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))
