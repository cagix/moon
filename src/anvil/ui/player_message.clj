(ns anvil.ui.player-message
  (:require [gdl.graphics :as g :refer [draw-text]]
            [gdl.stage :refer [message-to-player player-message-duration-seconds]]
            [gdl.ui :refer [ui-actor]]
            [gdl.utils :refer [bind-root]]))

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (draw-text {:x (/ g/viewport-width 2)
                :y (+ (/ g/viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (g/delta-time))
    (when (>= counter player-message-duration-seconds)
      (bind-root message-to-player nil))))

(defn actor []
  (ui-actor {:draw draw-player-message
             :act check-remove-message}))
