(ns anvil.widgets.player-message
  (:require [anvil.widgets :as widgets]
            [cdq.context :refer [message-to-player player-message-duration-seconds]]
            [anvil.app :as app]
            [gdl.context :as c]
            [gdl.ui :refer [ui-actor]]))

(defn- draw-player-message [{:keys [gdl.context/viewport] :as c}]
  (when-let [{:keys [message]} message-to-player]
    (c/draw-text c
                 {:x (/ (:width viewport) 2)
                  :y (+ (/ (:height viewport) 2) 200)
                  :text message
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message [c]
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (c/delta-time c))
    (when (>= counter player-message-duration-seconds)
      (bind-root message-to-player nil))))

(defn-impl widgets/player-message []
  (ui-actor {:draw #(draw-player-message  @app/state)
             :act  #(check-remove-message @app/state)}))
