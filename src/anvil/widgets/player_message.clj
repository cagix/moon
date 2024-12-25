(ns anvil.widgets.player-message
  (:require [anvil.widgets :as widgets]
            [clojure.gdx.graphics :as g]
            [gdl.context :as ctx]
            [gdl.graphics :refer [draw-text]]
            [gdl.stage :refer [message-to-player player-message-duration-seconds]]
            [gdl.ui :refer [ui-actor]])
  (:import (com.badlogic.gdx Gdx)))

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (draw-text (ctx/get-ctx)
               {:x (/ ctx/viewport-width 2)
                :y (+ (/ ctx/viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (g/delta-time Gdx/graphics))
    (when (>= counter player-message-duration-seconds)
      (bind-root message-to-player nil))))

(defn-impl widgets/player-message []
  (ui-actor {:draw draw-player-message
             :act check-remove-message}))
