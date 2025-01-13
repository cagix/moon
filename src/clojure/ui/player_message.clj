(ns clojure.ui.player-message
  (:require [clojure.context :as c]
            [clojure.graphics :as graphics]
            [clojure.ui :refer [ui-actor]]))

(defn- draw-player-message [{:keys [clojure.graphics/ui-viewport
                                    clojure.context/player-message] :as c}]
  (when-let [text (:text @player-message)]
    (c/draw-text c
                 {:x (/ (:width ui-viewport) 2)
                  :y (+ (/ (:height ui-viewport) 2) 200)
                  :text text
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message [{:keys [clojure.context/player-message]}]
  (when (:text @player-message)
    (swap! player-message update :counter + (graphics/delta-time))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn actor [_context _config]
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn create* [_context config]
  (atom {:duration-seconds (:duration-seconds config)}))
