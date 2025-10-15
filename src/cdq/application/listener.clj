(ns cdq.application.listener
  (:require [cdq.game :as game]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]))

(def state (atom nil))

(defn create [config]
  (listener/create
   {:create (fn []
              (reset! state (game/create! (gdx/context) config)))

    :dispose (fn []
               (game/dispose! @state))

    :render (fn []
              (swap! state game/render!))

    :resize (fn [width height]
              (game/resize! @state width height))

    :pause (fn [])
    :resume (fn [])}))
