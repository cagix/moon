(ns cdq.ui.player-message
  (:require [gdl.context :as c]
            [gdl.graphics :as graphics]
            [gdl.ui :refer [ui-actor]]))

(defn- draw-player-message [{:keys [gdl.graphics/ui-viewport
                                    cdq.context/player-message] :as c}]
  (when-let [text (:text @player-message)]
    (c/draw-text c
                 {:x (/ (:width ui-viewport) 2)
                  :y (+ (/ (:height ui-viewport) 2) 200)
                  :text text
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message [{:keys [cdq.context/player-message
                                     gdl/graphics]}]
  (when (:text @player-message)
    (swap! player-message update :counter + (graphics/delta-time graphics))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn create [_context]
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn create* [_context config]
  (atom {:duration-seconds config}))
