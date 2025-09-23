(ns cdq.application.desktop
  (:require [cdq.application]
            [cdq.application.listener]
            [clojure.config :as config]
            [gdl.application.desktop])
  (:gen-class))

(defn -main []
  (gdl.application.desktop/start!
   {:listener (cdq.application.listener/create
               {:state cdq.application/state
                :create-pipeline (config/edn-resource "create.edn")
                :render-pipeline (config/edn-resource "render.edn")})
    :config {:title "Cyber Dungeon Quest"
             :windowed-mode {:width 1440
                             :height 900}
             :foreground-fps 60}}))
