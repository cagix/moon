(ns anvil.widgets
  (:require [clojure.gdx :as gdx]
            [gdl.context :as c]
            [gdl.ui :as ui :refer [ui-actor]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]))

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

(defn action-bar-table [_context]
  (ui/table {:rows [[{:actor (action-bar)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- draw-player-message [{:keys [gdl.context/viewport
                                    cdq.context/player-message] :as c}]
  (when-let [text (:text @player-message)]
    (c/draw-text c
                 {:x (/ (:width viewport) 2)
                  :y (+ (/ (:height viewport) 2) 200)
                  :text text
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message [{:keys [cdq.context/player-message] :as c}]
  (when (:text @player-message)
    (swap! player-message update :counter + (gdx/delta-time c))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn player-message []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))
