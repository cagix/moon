(ns cdq.application.start
  (:require [cdq.application.os-specific-settings :as os-specific-settings]
            [cdq.application.lwjgl :as lwjgl-application])
  (:gen-class))

(defn -main []
  (os-specific-settings/handle!)
  (lwjgl-application/start!))
