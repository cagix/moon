(ns cdq.application
  (:require [cdq.game.create :as create]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [clojure.gdx.application :as application])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (application/start!
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
    }))
