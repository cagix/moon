(ns gdl.application
  (:require [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn start! [{::keys [create!
                       dispose!
                       render!
                       resize!
                       lwjgl-app-config]
               :as config}]
  (lwjgl/application lwjgl-app-config
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create! config)))

                       (dispose []
                         (dispose! @state))

                       (render []
                         (swap! state render!))

                       (resize [width height]
                         (resize! @state width height)))))
