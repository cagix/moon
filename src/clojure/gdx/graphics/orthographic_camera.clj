(ns clojure.gdx.graphics.orthographic-camera
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create
  ([]
   (OrthographicCamera.))
  ([{:keys [world-width world-height y-down?]}]
   (doto (OrthographicCamera.)
     (.setToOrtho y-down? world-width world-height))))
