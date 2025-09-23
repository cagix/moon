(ns cdq.application
  (:require [cdq.application.create :as create]
            [cdq.application.dispose :as dispose]
            [cdq.application.render :as render]
            [cdq.application.resize :as resize]
            [clojure.gdx.application :as application])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (application/start!
   {:listener {:create (fn [context]
                         (reset! state (create/do! context)))
               :dispose (fn []
                          (dispose/do! @state))
               :pause (fn [])
               :render (fn []
                         (swap! state render/do!))
               :resize (fn [width height]
                         (resize/do! @state width height))
               :resume (fn [])}
    :config {:title "Cyber Dungeon Quest"
             :windowed-mode {:width 1440
                             :height 900}
             :foreground-fps 60}}))
