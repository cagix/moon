(ns clojure.gdx.graphics.orthographic-camera
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create
  ([]
   (OrthographicCamera.))
  ([{:keys [world-width world-height y-down?]}]
   (doto (create)
     (.setToOrtho y-down? world-width world-height))))

(defn set-zoom! [^OrthographicCamera this amount]
  (set! (.zoom this) amount)
  (.update this))
