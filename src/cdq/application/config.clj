(ns cdq.application.config
  (:require [clojure.gdx.backends.lwjgl.application.config :as app-config]))

(defn create [opts]
  (app-config/create opts))
