(ns cdq.application.listener
  (:require [clojure.core-ext :refer [pipeline]]))

(def state (atom nil))

(defn create [config]
  {:create (fn []
             (reset! state ((:create config) com.badlogic.gdx.Gdx/app config)))

   :dispose (fn []
              ((:dispose config) @state))

   :render (fn []
             (swap! state pipeline (:render-pipeline config)))

   :resize (fn [width height]
             ((:resize config) @state width height))

   :pause (fn [])
   :resume (fn [])})
