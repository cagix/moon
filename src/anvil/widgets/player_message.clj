(ns anvil.widgets.player-message
  (:require [anvil.widgets :as widgets]
            [clojure.gdx.graphics :as g]
            [gdl.context :as ctx :refer [draw-text]]
            [gdl.stage :refer [message-to-player player-message-duration-seconds]]
            [gdl.ui :refer [ui-actor]])
  (:import (com.badlogic.gdx Gdx)))

(defn- draw-player-message [{:keys [gdl.context/viewport] :as c}]
  (when-let [{:keys [message]} message-to-player]
    (draw-text c
               {:x (/ (:width viewport) 2)
                :y (+ (/ (:height viewport) 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (g/delta-time Gdx/graphics))
    (when (>= counter player-message-duration-seconds)
      (bind-root message-to-player nil))))

(defn-impl widgets/player-message []
  (ui-actor {:draw (fn []
                     (draw-player-message @ctx/state))
             :act check-remove-message}))
