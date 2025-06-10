(ns cdq.ui.action-bar
  (:require [gdl.ui :as ui]
            [gdl.ui.button-group :as button-group]))

(defn- button-group-container []
  (ui/actor {:name "button-group-container"
             :user-object (button-group/create {:max-check-count 1
                                                :min-check-count 0})}))

(defn- horizontal-group []
  (ui/horizontal-group {:pad 2
                        :space 2
                        :user-object :horizontal-group
                        :actors [(button-group-container)]}))

(defn create [_ctx {:keys [id]}]
  (ui/table {:rows [[{:actor (horizontal-group)
                      :expand? true
                      :bottom? true}]]
             :id id
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- get-data [action-bar]
  (let [group (:horizontal-group action-bar)]
    {:horizontal-group group
     :button-group (ui/user-object (ui/find-actor group "button-group-container"))}))

(defn selected-skill [action-bar]
  (when-let [skill-button (button-group/checked (:button-group (get-data action-bar)))]
    (ui/user-object skill-button)))

(defn add-skill!
  [action-bar
   {:keys [skill-id
           texture-region
           tooltip-text]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (ui/image-button texture-region
                                (fn [_actor _ctx])
                                {:scale 2})]
    (ui/set-user-object! button skill-id)
    (ui/add-tooltip! button tooltip-text)
    (ui/add! horizontal-group button)
    (button-group/add! button-group button)
    nil))

(defn remove-skill! [action-bar skill-id]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group skill-id)]
    (ui/remove! button)
    (button-group/remove! button-group button)
    nil))
