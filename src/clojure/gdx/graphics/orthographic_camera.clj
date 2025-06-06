(ns clojure.gdx.graphics.orthographic-camera
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create
  ([]
   (proxy [OrthographicCamera ILookup] []
     (valAt [k]
       (let [this ^OrthographicCamera this]
         (case k
           :orthographic-camera/zoom (.zoom this)
           :camera/position [(.x (.position this))
                             (.y (.position this))]
           :camera/combined (.combined this)
           :camera/frustum (.frustum this)
           :camera/viewport-width (.viewportWidth this)
           :camera/viewport-height (.viewportHeight this))))))
  ([{:keys [world-width world-height y-down?]}]
   (doto (create)
     (OrthographicCamera/.setToOrtho y-down? world-width world-height))))

(defn set-zoom! [^OrthographicCamera this amount]
  (set! (.zoom this) amount)
  (.update this))
