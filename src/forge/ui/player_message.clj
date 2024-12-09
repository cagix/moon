(ns forge.ui.player-message
  (:require [anvil.graphics :refer [draw-text gui-viewport-width gui-viewport-height]]
            [anvil.stage :refer [message-to-player player-message-duration-seconds]]
            [anvil.ui :refer [ui-actor]]
            [clojure.gdx.graphics :refer [delta-time]]
            [clojure.utils :refer [bind-root]]))


(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (draw-text {:x (/ gui-viewport-width 2)
                :y (+ (/ gui-viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (delta-time))
    (when (>= counter player-message-duration-seconds)
      (bind-root message-to-player nil))))

(defn actor []
  (ui-actor {:draw draw-player-message
             :act check-remove-message}))
