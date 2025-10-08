(ns cdq.application
  (:require [cdq.game.create :as create]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [com.badlogic.gdx.backends.lwjgl :as lwjgl])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (lwjgl/application
   {
    :title "Cyber Dungeon Quest"

    :window {:width 1440
             :height 900}

    :fps 60

    :create! (fn [gdx]
               (reset! state (create/do! gdx)))

    :dispose! (fn []
                (dispose/do! @state))

    :render! (fn []
               (swap! state render/do!))

    :resize! (fn [width height]
               (resize/do! @state width height))

    :colors {"PRETTY_NAME" [0.84 0.8 0.52 1]}
    }))
