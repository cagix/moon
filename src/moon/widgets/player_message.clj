(ns moon.widgets.player-message
  (:require [gdl.graphics :as gdx.graphics]
            [gdl.ui :as ui]
            [moon.component :as component]
            [moon.graphics.text :as text]
            [moon.graphics.gui-view :as gui-view]))

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

(defc :widgets/player-message
  (component/create [_]
    (ui/actor {:draw draw-player-message
               :act check-remove-message})))

(defc :tx/msg-to-player
  (component/handle [[_ message]]
    (bind-root #'message-to-player {:message message :counter 0})
    nil))
