(ns cdq.ui.action-bar
  (:require [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.gdx.scene2d.ui.button-group :as button-group]
            [clojure.vis-ui.tooltip :as tooltip]
            [clojure.vis-ui.widget :as widget]))

(defn create [_ctx _]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/horizontal-group
                    :pad 2
                    :space 2
                    :actor/user-object :horizontal-group
                    :group/actors [{:actor/type :actor.type/actor
                                    :actor/name "button-group-container"
                                    :actor/user-object (button-group/create {:max-check-count 1
                                                                             :min-check-count 0})}]}
            :expand? true
            :bottom? true}]]
   :actor/user-object :action-bar
   :cell-defaults {:pad 2}
   :fill-parent? true})

(defn- get-data [action-bar]
  (let [group (:horizontal-group action-bar)]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "button-group-container"))}))

(defn selected-skill [action-bar]
  (when-let [skill-button (button-group/get-checked (:button-group (get-data action-bar)))]
    (actor/user-object skill-button)))

(defn add-skill!
  [action-bar
   {:keys [skill-id
           texture-region
           tooltip-text]}]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (widget/image-button {:drawable/texture-region texture-region
                                     :drawable/scale 2})]
    (actor/set-user-object! button skill-id)
    (tooltip/add!     button tooltip-text)
    (group/add!        horizontal-group button)
    (button-group/add! button-group     button)
    nil))

(defn remove-skill! [action-bar skill-id]
  (let [{:keys [horizontal-group button-group]} (get-data action-bar)
        button (get horizontal-group skill-id)]
    (actor/remove!                     button)
    (button-group/remove! button-group button)
    nil))
