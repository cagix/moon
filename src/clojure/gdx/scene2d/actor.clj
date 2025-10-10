(ns clojure.gdx.scene2d.actor
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn create [{:keys [act draw]}]
  (proxy [Actor] []
    (act [delta]
      (act this delta))
    (draw [batch parent-alpha]
      (draw this batch parent-alpha))))

(def stage            Actor/.getStage)
(def user-object      Actor/.getUserObject)
(def remove!          Actor/.remove)
(def set-name!        Actor/.setName)
(def set-user-object! Actor/.setUserObject)
(def set-visible!     Actor/.setVisible)
(def set-touchable!   Actor/.setTouchable)
(def add-listener!    Actor/.addListener)
(def set-position!    Actor/.setPosition)
(def width            Actor/.getWidth)
(def height           Actor/.getHeight)
