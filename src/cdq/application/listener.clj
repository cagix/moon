(ns cdq.application.listener
  (:require [cdq.game.create :as create]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]))

(def state (atom nil))

(defn create [config]
  (listener/create
   {:create (fn []
              (reset! state (create/do! (gdx/context) config)))

    :dispose (fn []
               (dispose/do! @state))

    :render (fn []
              (swap! state render/do!))

    :resize (fn [width height]
              (resize/do! @state width height))

    :pause (fn [])
    :resume (fn [])}))
