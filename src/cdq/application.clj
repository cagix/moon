(ns cdq.application
  (:require [cdq.application.create :as create]
            [cdq.application.dispose :as dispose]
            [cdq.application.render :as render]
            [cdq.application.resize :as resize]
            [clojure.application]
            [clojure.gdx.application :as application])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (application/start!
   {:listener (reify clojure.application/Listener
                (create [_ context]
                  (reset! state (create/do! context)))
                (dispose [_]
                  (dispose/do! @state))
                (pause [_])
                (render [_]
                  (swap! state render/do!))
                (resize [_ width height]
                  (resize/do! @state width height))
                (resume [_]))
    :config {:title "Cyber Dungeon Quest"
             :windowed-mode {:width 1440
                             :height 900}
             :foreground-fps 60}}))
