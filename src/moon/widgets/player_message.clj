(ns ^:no-doc moon.widgets.player-message
  (:require [gdl.graphics :as gdx.graphics]
            [moon.component :refer [defc] :as component]
            [moon.tx :as tx]
            [moon.graphics :as g]
            [gdl.ui :as ui]))

(def ^:private duration-seconds 1.5)

(def ^:private message-to-player nil)

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (g/draw-text {:x (/ (g/gui-viewport-width) 2)
                  :y (+ (/ (g/gui-viewport-height) 2) 200)
                  :text message
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (gdx.graphics/delta-time))
    (when (>= counter duration-seconds)
      (.bindRoot #'message-to-player nil))))

(defc :widgets/player-message
  (component/create [_]
    (ui/actor {:draw draw-player-message
               :act check-remove-message})))

(defc :tx/msg-to-player
  (tx/handle [[_ message]]
    (.bindRoot #'message-to-player {:message message :counter 0})
    nil))
