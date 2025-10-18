(ns cdq.application.listener
  (:require [clojure.core-ext :refer [pipeline]]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]))

(def state (atom nil))

(defn create [config]
  (listener/create
   {:create (fn []
              (reset! state ((:create config) (gdx/context) config)))

    :dispose (fn []
               ((:dispose config) @state))

    :render (fn []
              (swap! state pipeline (:render-pipeline config)))

    :resize (fn [width height]
              ((:resize config) @state width height))

    :pause (fn [])
    :resume (fn [])}))
