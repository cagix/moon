(ns cdq.create.stage.actionbar
  (:require [gdl.gdx.scenes.scene2d.actor :as actor]
            [cdq.ui :as ui :refer [ui-actor]]
            [gdl.gdx.scenes.scene2d.group :as group]))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (actor/set-user-object actor (ui/button-group {:max-check-count 1
                                                   :min-check-count 0}))
    actor))

(defn- action-bar []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (group/add-actor! group (action-bar-button-group))
    group))

(defn create [_context]
  (ui/table {:rows [[{:actor (action-bar)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))
