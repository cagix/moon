(ns gdx.backends.lwjgl.application
  (:require [clojure.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn start! [[config {:keys [create! dispose! render! resize!]}]]
  (lwjgl/application config
                     (proxy [ApplicationAdapter] []
                       (create []
                         (let [[f params] create!]
                           (f params)))

                       (dispose []
                         (dispose!))

                       (render []
                         (let [[f params] render!]
                           (f params)))

                       (resize [width height]
                         (resize! width height)))))
