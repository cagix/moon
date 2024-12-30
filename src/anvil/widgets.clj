(ns anvil.widgets
  (:require [gdl.ui :as ui :refer [ui-actor]]
            [gdl.ui.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn hp-mana-bar [c])

(defn dev-menu [c])

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (Actor/.setUserObject actor (ui/button-group {:max-check-count 1
                                                  :min-check-count 0}))
    actor))

(defn- action-bar []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (group/add-actor! group (action-bar-button-group))
    group))

(defn action-bar-table [_context]
  (ui/table {:rows [[{:actor (action-bar)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn inventory [c])

(defn set-item-image-in-widget [c cell item])

(defn remove-item-from-widget [c cell])

(defn entity-info-window [c])

(defn player-message [])
