(ns cdq.ctx.create.ui.action-bar
  (:require [cdq.ui.action-bar :as action-bar]
            [clojure.scene2d.vis-ui.image-button :as image-button])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)
           (com.badlogic.gdx.scenes.scene2d.ui Button
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
  (let [group (Group/.findActor action-bar "cdq.ui.action-bar.horizontal-group")]
    {:horizontal-group group
     :button-group (Actor/.getUserObject (Group/.findActor group "button-group-container"))}))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  action-bar/ActionBar
  (selected-skill [action-bar]
    (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-data action-bar)))]
      (Actor/.getUserObject skill-button)))

  (add-skill!
    [action-bar
     {:keys [skill-id
             texture-region
             tooltip-text]}]
    (let [{:keys [horizontal-group button-group]} (get-data action-bar)
          button (image-button/create
                  {:actor/user-object skill-id
                   :drawable/texture-region texture-region
                   :drawable/scale 2
                   :tooltip tooltip-text})]
      (Group/.addActor horizontal-group button)
      (ButtonGroup/.add button-group ^Button button)
      nil))

  (remove-skill! [action-bar skill-id]
    (let [{:keys [horizontal-group button-group]} (get-data action-bar)
          button (get horizontal-group skill-id)]
      (Actor/.remove                     button)
      (ButtonGroup/.remove button-group ^Button button)
      nil)))
