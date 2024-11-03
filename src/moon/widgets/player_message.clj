(ns moon.widgets.player-message
  (:require [gdl.graphics :as gdx.graphics]
            [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.text :as text]
            [gdl.ui :as ui]))

(def ^:private duration-seconds 1.5)

(def ^:private message-to-player nil)

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (text/draw {:x (/ (gui-view/width) 2)
                :y (+ (/ (gui-view/height) 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (gdx.graphics/delta-time))
    (when (>= counter duration-seconds)
      (bind-root #'message-to-player nil))))

(defn create [_]
  (ui/actor {:draw draw-player-message
             :act check-remove-message}))

(defn handle [[_ message]]
  (bind-root #'message-to-player {:message message :counter 0})
  nil)
