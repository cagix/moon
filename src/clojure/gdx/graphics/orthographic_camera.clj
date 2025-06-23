(ns clojure.gdx.graphics.orthographic-camera
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create
  ([]
   (OrthographicCamera.))
  ([& {:keys [y-down? world-width world-height]}]
   (doto (OrthographicCamera.)
     (.setToOrtho y-down? world-width world-height))))
