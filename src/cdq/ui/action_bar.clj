(ns cdq.ui.action-bar
  (:require [gdx.ui.actor :as actor]
            [gdl.ui.button-group :as button-group]
            [gdx.ui.group :as group]
            [gdx.ui :as ui]))

(defn create [_ctx {:keys [id]}]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/horizontal-group
                    :pad 2
                    :space 2
                    :user-object :horizontal-group
                    :actors [{:actor/type :actor.type/actor
                              :name "button-group-container"
                              :user-object (button-group/create {:max-check-count 1
                                                                 :min-check-count 0})}]}
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
  (when-let [skill-button (button-group/checked (:button-group (get-data action-bar)))]
    (actor/user-object skill-button)))

(defn add-skill!
  [action-bar
   {:keys [skill-id
           texture-region
           tooltip-text]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (ui/image-button {:texture-region texture-region
                                 :scale 2})]
    (actor/set-user-object! button skill-id)
    (actor/add-tooltip!     button tooltip-text)
    (group/add!        horizontal-group button)
    (button-group/add! button-group     button)
    nil))

(defn remove-skill! [action-bar skill-id]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group skill-id)]
    (actor/remove!                     button)
    (button-group/remove! button-group button)
    nil))
