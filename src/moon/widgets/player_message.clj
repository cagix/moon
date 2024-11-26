(ns moon.widgets.player-message
  (:require [clojure.gdx :as gdx]
            [forge.ui :as ui]
            [forge.graphics :refer [draw-text gui-viewport-width gui-viewport-height]]))

(def ^:private duration-seconds 1.5)

(def ^:private message-to-player nil)

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (draw-text {:x (/ gui-viewport-width 2)
                :y (+ (/ gui-viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (gdx/delta-time))
    (when (>= counter duration-seconds)
      (.bindRoot #'message-to-player nil))))

(defn create []
  (ui/actor {:draw draw-player-message
             :act check-remove-message}))

(defn show [message]
  (.bindRoot #'message-to-player {:message message :counter 0}))
