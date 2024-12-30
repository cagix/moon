(ns anvil.widgets.player-message
  (:require [anvil.widgets :as widgets]
            [clojure.gdx :as gdx]
            [gdl.context :as c]
            [gdl.ui :refer [ui-actor]]))

(defn- draw-player-message [{:keys [gdl.context/viewport
                                    cdq.context/player-message] :as c}]
  (when-let [text (:text @player-message)]
    (c/draw-text c
                 {:x (/ (:width viewport) 2)
                  :y (+ (/ (:height viewport) 2) 200)
                  :text text
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message [{:keys [cdq.context/player-message] :as c}]
  (when (:text @player-message)
    (swap! player-message update :counter + (gdx/delta-time c))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn-impl widgets/player-message []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))
