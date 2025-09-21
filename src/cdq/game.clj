(ns cdq.game
  (:require [cdq.application :as application]
            [cdq.application.create :as create]
            [cdq.application.dispose :as dispose]
            [cdq.application.render :as render]
            [cdq.application.resize :as resize]
            [gdl.backends.desktop :as desktop])
  (:gen-class))

(defn -main []
  (desktop/application
   {:title "Cyber Dungeon Quest"
    :windowed-mode {:width 1440
                    :height 900}
    :foreground-fps 60
    :listener {:create (fn [context]
                         (reset! application/state (create/do! context)))
               :dispose (fn []
                          (dispose/do! @application/state))
               :render (fn []
                         (render/do! application/state))
               :resize (fn [width height]
                         (resize/do! @application/state width height))
               :pause (fn [])
               :resume (fn [])}
    :mac {:glfw-async? true
          :taskbar-icon "icon.png"}}))
