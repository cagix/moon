(ns cdq.ctx.create.ui.action-bar
  (:require [cdq.ui.action-bar :as action-bar]
            [cdq.ui.tooltip :as tooltip]
            [clojure.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button
                                               ButtonGroup)))

(defn create [_ctx]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/horizontal-group
                    :pad 2
                    :space 2
                    :actor/name "cdq.ui.action-bar.horizontal-group"
                    :group/actors [{:actor/type :actor.type/actor
                                    :actor/name "button-group-container"
                                    :actor/user-object (doto (ButtonGroup.)
                                                         (.setMaxCheckCount 1)
                                                         (.setMinCheckCount 0))}]}
            :expand? true
            :bottom? true}]]
   :actor/name "cdq.ui.action-bar"
   :cell-defaults {:pad 2}
   :fill-parent? true})

(defn- get-data [action-bar]
  (let [group (group/find-actor action-bar "cdq.ui.action-bar.horizontal-group")]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "button-group-container"))}))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  action-bar/ActionBar
  (selected-skill [action-bar]
    (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-data action-bar)))]
      (actor/user-object skill-button)))

  (add-skill!
    [action-bar
     {:keys [skill-id
             texture-region
             tooltip-text]}]
    (let [{:keys [horizontal-group button-group]} (get-data action-bar)
          button (scene2d/build
                  {:actor/type :actor.type/image-button
                   :actor/user-object skill-id
                   :drawable/texture-region texture-region
                   :drawable/scale 2})]
      (tooltip/add! button tooltip-text)
      (group/add-actor! horizontal-group button)
      (ButtonGroup/.add button-group ^Button button)
      nil))

  (remove-skill! [action-bar skill-id]
    (let [{:keys [horizontal-group button-group]} (get-data action-bar)
          button (get horizontal-group skill-id)]
      (actor/remove!                     button)
      (ButtonGroup/.remove button-group ^Button button)
      nil)))
