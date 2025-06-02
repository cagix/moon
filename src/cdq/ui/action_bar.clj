(ns cdq.ui.action-bar
  (:require [clojure.ctx :as ctx]
            [clojure.ui :as ui]
            [clojure.ui.button-group :as button-group]))

(defn- button-group-container []
  (ui/actor {:name "button-group-container"
             :user-object (button-group/create {:max-check-count 1
                                                :min-check-count 0})}))

(defn- horizontal-group []
  (ui/horizontal-group {:pad 2
                        :space 2
                        :user-object :horizontal-group
                        :actors [(button-group-container)]}))

(defn create [_ctx]
  (ui/table {:rows [[{:actor (horizontal-group)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- get-data [action-bar]
  (let [group (:horizontal-group action-bar)]
    {:horizontal-group group
     :button-group (ui/user-object (ui/find-actor group "button-group-container"))}))

(defn selected-skill [action-bar]
  (when-let [skill-button (button-group/checked (:button-group (get-data action-bar)))]
    (ui/user-object skill-button)))

(defn add-skill! [action-bar {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (ui/image-button image
                                (fn [_actor _ctx])
                                {:scale 2})]
    (ui/set-user-object! button id)
    (ui/add-tooltip! button #(ctx/info-text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (ui/add! horizontal-group button)
    (button-group/add! button-group button)
    nil))

(defn remove-skill! [action-bar {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group id)]
    (ui/remove! button)
    (button-group/remove! button-group button)
    nil))

(comment
 (keys (:entity/skills @clojure.ctx/player-eid))

 (remove-skill! (:action-bar clojure.ctx/stage) {:property/id :skills/spawn}))
